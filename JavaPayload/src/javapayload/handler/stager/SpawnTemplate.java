/*
 * Spawn Java Payloads.
 * 
 * Copyright (c) 2010, Michael 'mihi' Schierl.
 * All rights reserved.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package javapayload.handler.stager;

import java.io.PrintStream;

import javapayload.handler.stage.StageHandler;

public class SpawnTemplate extends StagerHandler {

	StagerHandler handler = new LocalTest();
	
	protected void handle(StageHandler stageHandler, String[] parameters, PrintStream errorStream, Object extraArg) throws Exception {
		handler.handle(stageHandler, parameters, errorStream, extraArg);
	}
	
	protected String getTestArguments() {
		return handler.getTestArguments();
	}
	
	protected boolean prepare(String[] parametersToPrepare) throws Exception {
		
		boolean fCutSpawn = parametersToPrepare.length >= 1 && parametersToPrepare[0].startsWith("Spawn");
		if (fCutSpawn)
			parametersToPrepare[0] = parametersToPrepare[0].substring(5);
		try {
			return handler.prepare(parametersToPrepare);
		} finally {
			if (fCutSpawn)
				parametersToPrepare[0] = "Spawn" + parametersToPrepare[0];
		}
	}

	protected boolean needHandleBeforeStart() {
		return handler.needHandleBeforeStart();
	}
}
