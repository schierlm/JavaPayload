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

package javapayload;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public abstract class HandlerModule extends Module {

	public static HandlerModule[] loadAll(Class moduleType, boolean forHandler) throws Exception {
		return loadAll(moduleType, forHandler, !forHandler);
	}
	
	public static HandlerModule[] loadAll(Class moduleType, boolean requireHandler, boolean requireTarget) throws Exception {
		Module[] unfiltered = Module.loadAll(moduleType);
		List filtered = new ArrayList();
		for (int i = 0; i < unfiltered.length; i++) {
			HandlerModule module = (HandlerModule)unfiltered[i];
			if ((!requireHandler || module.isHandlerUsable()) && (!requireTarget || module.isTargetUsable()))
				filtered.add(module);
		}
		return (HandlerModule[]) filtered.toArray(new HandlerModule[filtered.size()]);
	}
	
	public static void list(PrintStream out, Class moduleType, boolean forHandler) throws Exception {
		printList(out, loadAll(moduleType, forHandler));
	}
	
	public static void list(PrintStream out, Class moduleType, boolean requireHandler, boolean requireTarget) throws Exception {
		printList(out, loadAll(moduleType, requireHandler, requireTarget));
	}
	
	private final boolean handlerUsable;
	private final boolean targetUsable;
	
	public HandlerModule(Class moduleType, boolean handlerUsable, boolean targetUsable, String summary, String description) {
		super(null, moduleType, summary, description);
		this.handlerUsable = handlerUsable;
		this.targetUsable = targetUsable;
	}
	
	public boolean isHandlerUsable() {
		return handlerUsable;
	}
	
	public boolean isTargetUsable() {
		return targetUsable;
	}
}
