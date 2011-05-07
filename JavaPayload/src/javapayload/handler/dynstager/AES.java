package javapayload.handler.dynstager;

import java.io.PrintStream;

import javapayload.handler.stage.StageHandler;
import javapayload.handler.stager.DynStagerHandler;
import javapayload.handler.stager.StagerHandler;

public class AES extends DynStagerHandler {
	
	protected void handleDyn(StageHandler stageHandler, String[] parameters, PrintStream errorStream, Object extraArg, StagerHandler readyHandler) throws Exception {
		String[] newParameters = new String[parameters.length - 1];
		System.arraycopy(parameters, 2, newParameters, 1, newParameters.length - 1);
		newParameters[0] = parameters[0];
		super.handleDyn(new AESStageHandler(parameters[1], stageHandler), newParameters, errorStream, extraArg, readyHandler);
	}

	protected boolean prepare(String[] parametersToPrepare) throws Exception {
		String[] temp = new String[parametersToPrepare.length - 1];
		System.arraycopy(parametersToPrepare, 2, temp, 1, temp.length - 1);
		temp[0] = parametersToPrepare[0].substring(3);
		boolean changed = super.prepare(temp);
		if (changed)
			System.arraycopy(temp, 1, parametersToPrepare, 2, temp.length - 1);
		if (parametersToPrepare[1].equals("#")) {
			parametersToPrepare[1] = AESStageHandler.generatePassword();
			changed = true;
		}
		return changed;
	}

	public final String[] getTestArgumentArray() {
		String[] origHandlerArgs = super.getTestArgumentArray();
		if (origHandlerArgs == null)
			return null;
		String[] handlerArgs = new String[origHandlerArgs.length];
		for (int i = 0; i < handlerArgs.length; i++) {
			handlerArgs[i] = "# "+origHandlerArgs[i];
		}
		return handlerArgs;
	}
}
