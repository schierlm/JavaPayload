/*
 * Java Payloads.
 * 
 * Copyright (c) 2011 Michael 'mihi' Schierl
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *   
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *   
 * - Neither name of the copyright holders nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *   
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND THE CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR THE CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package javapayload.cli.param;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javapayload.HandlerModule;
import javapayload.Module;
import javapayload.NamedElement;
import javapayload.Parameter;
import javapayload.builder.Builder;
import javapayload.builder.Discovery;
import javapayload.builder.Injector;
import javapayload.cli.Command;
import javapayload.cli.HandlerCommand;
import javapayload.cli.InjectorCommand;
import javapayload.cli.ShowCommand.ModuleType;
import javapayload.cli.StagerCommand;
import javapayload.handler.dynstager.DynStagerHandler;
import javapayload.handler.stage.StageHandler;
import javapayload.handler.stager.ListeningStagerHandler;
import javapayload.handler.stager.StagerHandler;

class ModuleParameterHandler extends ParameterHandler {

	protected final ParameterContext context;
	protected final ModuleType moduleType;
	private final boolean withArgs;

	protected ModuleParameterHandler(ParameterContext context, Parameter parameter, ModuleType moduleType, boolean withArgs) {
		super(parameter);
		this.context = context;
		this.moduleType = moduleType;
		this.withArgs = withArgs;
	}

	public int getRepeatCount() {
		return 0;
	}

	public NamedElement[] getPossibleValues() {
		try {
			return Module.loadAll(moduleType.getModuleClass());
		} catch (Exception ex) {
			throw new RuntimeException("Error while loading module list", ex);
		}
	}

	protected char getSeparator(Module module) {
		return ' ';
	}

	protected String getPrefix() {
		return "";
	}

	public ParameterHandler[] getNextHandlers(String input, StringBuffer commandBuffer) throws ParameterFormatException {
		if (input.length() == 0) {
			if (!isOptional())
				throw new ParameterFormatException("Parameter is not optional");
			return getNextHandlersOptional();
		}
		Module module;
		try {
			module = Module.load(moduleType.getModuleClass(), moduleType.getModuleName(input));
			boolean ok = false;
			NamedElement[] modules = getPossibleValues();
			for (int i = 0; i < modules.length; i++) {
				if (modules[i].getName().equals(module.getName())) {
					ok = true;
					break;
				}
			}
			if (!ok)
				throw new ParameterFormatException("Module not found");
		} catch (IllegalArgumentException ex) {
			throw new ParameterFormatException("Module not found");
		} catch (Exception ex) {
			throw new RuntimeException("Error while loading module", ex);
		}
		commandBuffer.append(getPrefix()).append(input).append(getSeparator(module));
		return getNextHandlers(module);
	}

	protected ParameterHandler[] getNextHandlersOptional() {
		return new ParameterHandler[0];
	}

	protected ParameterContext getNewContext(Module module) {
		return context;
	}

	protected ParameterHandler[] getNextHandlers(Module module) {
		if (!withArgs)
			return new ParameterHandler[0];
		Parameter[] parameters = module.getParameters();
		ParameterContext context = getNewContext(module);
		ParameterHandler[] result = new ParameterHandler[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			if (i == 1 && parameters.length == 2 && parameters[i].getType() == Command.TYPE_MODULE_BY_TYPE && result[0] instanceof ModuleTypeParameterHandler) {
				((ModuleTypeParameterHandler) result[0]).setModuleByTypeParam(parameters[1]);
				result = new ParameterHandler[] { result[0] };
				break;
			}
			if (i == 1 && parameters.length == 3 && parameters[i].getType() == Command.TYPE_STAGER_AND_HANDLER && result[0] instanceof InjectorParameterHandler) {
				((InjectorParameterHandler) result[0]).setStagerHandlerParam(parameters[1]);
				result = new ParameterHandler[] {
						result[0],
						getHandler(context, parameters[2])
				};
				break;
			}
			result[i] = getHandler(context, parameters[i]);
		}
		return result;
	}

	protected static void registerHandlers() {

		TypeHandler listeningStagerHandlerType = new TypeHandler() {
			public ParameterHandler getHandler(ParameterContext context, Parameter param) {
				return new RealStagerParameterHandler(context, param, new DynStagerHandler[0], new Class[] { null }, true, false) {
					public NamedElement[] getPossibleValues() {
						NamedElement[] unfiltered = super.getPossibleValues();
						List filtered = new ArrayList();
						for (int i = 0; i < unfiltered.length; i++) {
							StagerHandler handler = (StagerHandler) unfiltered[i];
							if (handler instanceof ListeningStagerHandler) {
								filtered.add(handler);
							}
						}
						return (Module[]) filtered.toArray(new Module[filtered.size()]);
					}
				};
			}
		};
		TypeHandler commandType = new TypeHandler() {
			public ParameterHandler getHandler(ParameterContext context, Parameter param) {
				return new ModuleParameterHandler(context, param, new ModuleType(Command.class, "Command", ""), false);
			}
		};
		TypeHandler moduleTypeType = new TypeHandler() {
			public ParameterHandler getHandler(ParameterContext context, Parameter param) {
				return new ModuleTypeParameterHandler(context, param);
			}
		};
		TypeHandler outOfContextType = new TypeHandler() {
			public ParameterHandler getHandler(ParameterContext context, Parameter param) {
				throw new IllegalStateException("Parameter type is used in a context where it is not supported");
			}
		};
		TypeHandler discoveryType = new TypeHandler() {
			public ParameterHandler getHandler(ParameterContext context, Parameter param) {
				return new ModuleParameterHandler(context, param, new ModuleType(Discovery.class, "Discovery", null), true);
			}
		};
		TypeHandler injectorType = new TypeHandler() {
			public ParameterHandler getHandler(ParameterContext context, Parameter param) {
				return new InjectorParameterHandler(context, param);
			}
		};
		TypeHandler builderType = new TypeHandler() {
			public ParameterHandler getHandler(ParameterContext context, Parameter param) {
				return new ModuleParameterHandler(context, param, new ModuleType(Builder.class, "Builder", null), false) {
					protected ParameterHandler[] getNextHandlers(Module module) {
						Builder builder = (Builder) module;
						ParameterHandler[] result = new ParameterHandler[builder.getMinParameterCount()];
						ParameterHandler argHandler = ParameterHandler.getHandler(this.context, new Parameter("BUILDERARG", false, Parameter.TYPE_ANY, "Any mandatory builder argument"));
						Arrays.fill(result, argHandler);
						return result;
					}
				};
			}
		};

		registerTypeHandler(Parameter.TYPE_STAGE, new StageTypeHandler(""));
		registerTypeHandler(Parameter.TYPE_STAGE_3DASHES, new StageTypeHandler("--- "));
		registerTypeHandler(Parameter.TYPE_STAGER, new StagerTypeHandler(false, true, new Class[] { null }));
		registerTypeHandler(Parameter.TYPE_LISTENING_STAGER_HANDLER, listeningStagerHandlerType);
		registerTypeHandler(Command.TYPE_COMMAND, commandType);
		registerTypeHandler(Command.TYPE_STAGER_HANDLER, new StagerTypeHandler(true, false, new Class[] { null }));
		registerTypeHandler(Command.TYPE_STAGER_AND_HANDLER, outOfContextType);
		registerTypeHandler(Command.TYPE_STAGE_2DASHES, new StageTypeHandler("-- "));
		registerTypeHandler(Command.TYPE_MODULETYPE, moduleTypeType);
		registerTypeHandler(Command.TYPE_MODULE_BY_TYPE, outOfContextType);
		registerTypeHandler(Command.TYPE_DISCOVERY, discoveryType);
		registerTypeHandler(Command.TYPE_INJECTOR, injectorType);
		registerTypeHandler(Command.TYPE_BUILDER, builderType);
	}

	private static class HandlerModuleParameterHandler extends ModuleParameterHandler {

		protected final boolean handler;
		protected final boolean target;

		public HandlerModuleParameterHandler(ParameterContext context, Parameter parameter, ModuleType moduleType, boolean handler, boolean target) {
			super(context, parameter, moduleType, true);
			this.handler = handler;
			this.target = target;
		}

		public NamedElement[] getPossibleValues() {
			try {
				return HandlerModule.loadAll(moduleType.getModuleClass(), handler, target);
			} catch (Exception ex) {
				throw new RuntimeException("Error while loading module list", ex);
			}
		}
	}

	private static class StageParameterHandler extends HandlerModuleParameterHandler {
		private final String prefix;

		public StageParameterHandler(Parameter parameter, ParameterContext context, String prefix) {
			super(context, parameter, new ModuleType(StageHandler.class, "", null), !context.equals(ParameterContext.STAGER), !context.equals(ParameterContext.HANDLER));
			this.prefix = prefix;
			if (!context.equals(ParameterContext.HANDLER) && !context.equals(ParameterContext.STAGER) && !context.equals(ParameterContext.INJECTOR))
				throw new IllegalStateException("Unsupported context for parameter type");
		}

		protected String getPrefix() {
			return prefix;
		}
	}

	private static class StageTypeHandler implements TypeHandler {
		private final String prefix;

		public StageTypeHandler(String prefix) {
			this.prefix = prefix;
		}

		public ParameterHandler getHandler(ParameterContext context, Parameter param) {
			return new StageParameterHandler(param, context, prefix);
		}
	}

	private static class StagerParameterHandler extends HandlerModuleParameterHandler {

		private Parameter origParameter;
		private final DynStagerHandler[] previousDynstagers;
		private final Class[] extraArgTypes;

		public StagerParameterHandler(ParameterContext context, Parameter parameter, boolean handler, boolean stager, Class[] extraArgTypes) {
			this(context, parameter, handler, stager, new DynStagerHandler[0], extraArgTypes);
		}

		private StagerParameterHandler(ParameterContext context, Parameter origParameter, boolean handler, boolean stager, DynStagerHandler[] previousDynstagers, Class[] extraArgTypes) {
			super(context, new Parameter("DYN" + origParameter.getName(), true, origParameter.getType(), "Dynstager for: " + origParameter.getSummary()), new ModuleType(DynStagerHandler.class, "", null), handler, stager);
			this.origParameter = origParameter;
			this.previousDynstagers = previousDynstagers;
			this.extraArgTypes = extraArgTypes;
		}

		public int getRepeatCount() {
			return previousDynstagers.length;
		}

		public NamedElement[] getPossibleValues() {
			try {
				NamedElement[] unfiltered = super.getPossibleValues();
				if (previousDynstagers.length == 0) {
					return unfiltered;
				} else {
					List filtered = new ArrayList();
					for (int i = 0; i < unfiltered.length; i++) {
						DynStagerHandler module = (DynStagerHandler) unfiltered[i];
						if (module.isDynstagerUsableWith(previousDynstagers))
							filtered.add(module);
					}
					return (HandlerModule[]) filtered.toArray(new HandlerModule[filtered.size()]);
				}
			} catch (Exception ex) {
				throw new RuntimeException("Error while loading module list", ex);
			}
		}

		protected ParameterHandler[] getNextHandlers(Module module) {
			DynStagerHandler dsh = (DynStagerHandler) module;
			List prevDynstagers = new ArrayList(Arrays.asList(previousDynstagers));
			prevDynstagers.add(dsh);
			StagerParameterHandler nextHandler = new StagerParameterHandler(context, origParameter, handler, target, (DynStagerHandler[]) prevDynstagers.toArray(new DynStagerHandler[prevDynstagers.size()]), extraArgTypes);
			if (dsh.getExtraArg() != null) {
				if (dsh.getExtraArg().isOptional())
					throw new IllegalStateException("Dynstager extra arg may not be optional");
				return new ParameterHandler[] {
						new SimpleParameterHandler(dsh.getExtraArg()) {
							protected void validate(String input) throws ParameterFormatException {
								for (int i = 0; i < input.length(); i++) {
									char ch = input.charAt(i);
									if (ch == '_' || ch == '$' || !Character.isJavaIdentifierPart(ch))
										throw new ParameterFormatException("Invalid character '" + ch + "' in dynstager argument");
								}
							}

							protected char getSeparator() {
								return '_';
							}
						},
						nextHandler
				};
			} else {
				return new ParameterHandler[] {
						nextHandler
				};
			}
		}

		protected char getSeparator(Module module) {
			if (((DynStagerHandler) module).getExtraArg() != null)
				return '$';
			else
				return '_';
		}

		protected ParameterHandler[] getNextHandlersOptional() {
			return new ParameterHandler[] {
					new RealStagerParameterHandler(context, origParameter, previousDynstagers, extraArgTypes, handler, target)
			};
		}
	}

	private static class RealStagerParameterHandler extends HandlerModuleParameterHandler {
		private final DynStagerHandler[] dynstagers;
		private final Class[] extraArgTypes;

		public RealStagerParameterHandler(ParameterContext context, Parameter parameter, DynStagerHandler[] dynstagers, Class[] extraArgTypes, boolean handler, boolean stager) {
			super(context, parameter, new ModuleType(StagerHandler.class, "", null), handler, stager);
			this.dynstagers = dynstagers;
			this.extraArgTypes = extraArgTypes;
		}

		public NamedElement[] getPossibleValues() {

			try {
				List firstFilter = new ArrayList();
				if (!target || dynstagers.length == 0) {
					firstFilter.addAll(Arrays.asList(super.getPossibleValues()));
				} else {
					Module[] unfiltered = Module.loadAll(moduleType.getModuleClass());
					for (int i = 0; i < unfiltered.length; i++) {
						StagerHandler module = (StagerHandler) unfiltered[i];
						if ((!handler || module.isHandlerUsable()) && module.isStagerUsableWith(dynstagers))
							firstFilter.add(module);
					}
				}
				List secondFilter = new ArrayList();
				for (int i = 0; i < firstFilter.size(); i++) {
					StagerHandler module = (StagerHandler) firstFilter.get(i);
					StagerHandler.Loader loader = new StagerHandler.Loader(new String[] { module.getName(), "--", "JSh" });
					for (int j = 0; j < extraArgTypes.length; j++) {
						if (loader.canHandleExtraArg(extraArgTypes[j])) {
							secondFilter.add(module);
							break;
						}
					}
				}
				return (HandlerModule[]) secondFilter.toArray(new HandlerModule[secondFilter.size()]);
			} catch (Exception ex) {
				throw new RuntimeException("Error while loading module list", ex);
			}
		}

		protected ParameterHandler[] getNextHandlers(Module module) {
			List allHandlers = new ArrayList();
			for (int i = 0; i < dynstagers.length; i++) {
				allHandlers.addAll(Arrays.asList(super.getNextHandlers(dynstagers[i])));
			}
			allHandlers.addAll(Arrays.asList(super.getNextHandlers(module)));
			return (ParameterHandler[]) allHandlers.toArray(new ParameterHandler[allHandlers.size()]);
		}
	}

	private static class StagerTypeHandler implements TypeHandler {
		private boolean handler;
		private boolean stager;
		private final Class[] extraArgTypes;

		public StagerTypeHandler(boolean handler, boolean stager, Class[] extraArgTypes) {
			this.handler = handler;
			this.stager = stager;
			this.extraArgTypes = extraArgTypes;
		}

		public ParameterHandler getHandler(ParameterContext context, Parameter param) {
			return new StagerParameterHandler(context, param, handler, stager, extraArgTypes);
		}
	}

	private static class InjectorParameterHandler extends ModuleParameterHandler {

		private Parameter param = null;

		public InjectorParameterHandler(ParameterContext context, Parameter param) {
			super(context, param, new ModuleType(Injector.class, "Injector", null), true);
		}

		public void setStagerHandlerParam(Parameter parameter) {
			param = parameter;
		}

		protected ParameterHandler[] getNextHandlers(Module module) {
			ParameterHandler[] result = super.getNextHandlers(module);
			if (param != null) {
				ParameterHandler[] origResult = result;
				result = new ParameterHandler[result.length + 1];
				System.arraycopy(origResult, 0, result, 0, origResult.length);
				result[origResult.length] = new StagerTypeHandler(true, true, ((Injector) module).getSupportedExtraArgClasses()).getHandler(context, param);
			}
			return result;
		}
	}

	private static class ModuleTypeParameterHandler extends ParameterHandler {

		Parameter moduleByTypeParam;
		private final ParameterContext context;

		public ModuleTypeParameterHandler(ParameterContext context, Parameter param) {
			super(param);
			this.context = context;
		}

		public void setModuleByTypeParam(Parameter moduleByTypeParam) {
			this.moduleByTypeParam = moduleByTypeParam;
		}

		public int getRepeatCount() {
			return 0;
		}

		public NamedElement[] getPossibleValues() {
			return new NamedElement[] {
					new ModuleTypeInfo("builder", new ModuleType(Builder.class, "Builder", null), "Builder"),
					new ModuleTypeInfo("command", new ModuleType(Command.class, "Command", null), "Command"),
					new ModuleTypeInfo("discovery", new ModuleType(Discovery.class, "Discovery", null), "Discovery module"),
					new ModuleTypeInfo("dynstager", new ModuleType(DynStagerHandler.class, "", null), "Dynstager"),
					new ModuleTypeInfo("injector", new ModuleType(Injector.class, "Injector", null), "Injector"),
					new ModuleTypeInfo("stage", new ModuleType(StageHandler.class, "", null), "Stage or stage handler"),
					new ModuleTypeInfo("stager", new ModuleType(StagerHandler.class, "", null), "stager or stager handler"),
			};
		}

		public ParameterHandler[] getNextHandlers(String input, StringBuffer commandBuffer) throws ParameterFormatException {
			if (input.length() == 0) {
				if (!isOptional())
					throw new ParameterFormatException("Parameter is not optional");
				return new ParameterHandler[0];
			}
			ModuleType moduleType = null;
			NamedElement[] values = getPossibleValues();
			for (int i = 0; i < values.length; i++) {
				ModuleTypeInfo mti = (ModuleTypeInfo) values[i];
				if (mti.getName().equalsIgnoreCase(input)) {
					moduleType = mti.getType();
					break;
				}
			}
			if (moduleType == null) {
				throw new ParameterFormatException("Invalid module type");
			}
			commandBuffer.append(input).append(' ');
			if (moduleByTypeParam == null) {
				return new ParameterHandler[0];
			} else {
				return new ParameterHandler[] {
						new ModuleParameterHandler(context, moduleByTypeParam, moduleType, false)
				};
			}
		}

		private static class ModuleTypeInfo implements NamedElement {
			private final String name;
			private final ModuleType type;
			private final String summary;

			public ModuleTypeInfo(String name, ModuleType type, String summary) {
				this.name = name;
				this.type = type;
				this.summary = summary;
			}

			public String getName() {
				return name;
			}

			public ModuleType getType() {
				return type;
			}

			public String getSummary() {
				return summary;
			}
		}
	}

	public static class RootParameterHandler extends ModuleParameterHandler {
		public RootParameterHandler(Parameter parameter) {
			super(ParameterContext.NORMAL, parameter, new ModuleType(Command.class, "Command", null), true);
		}

		protected ParameterContext getNewContext(Module module) {
			Command cmd = (Command) module;

			if (cmd instanceof StagerCommand)
				return ParameterContext.STAGER;
			if (cmd instanceof HandlerCommand)
				return ParameterContext.HANDLER;
			if (cmd instanceof InjectorCommand)
				return ParameterContext.INJECTOR;
			return context;
		}
	}
}