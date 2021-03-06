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

public class Parameter implements NamedElement {

	public static final int TYPE_ANY = 0;
	public static final int TYPE_HOST = 1;
	public static final int TYPE_PORT = 2;
	public static final int TYPE_PATH = 3;
	public static final int TYPE_URL = 4;
	public static final int TYPE_NUMBER = 5;
	public static final int TYPE_PORT_HASH = 6;
	
	// types that need special treatment
	public static final int TYPE_STAGE = 50;
	public static final int TYPE_STAGE_3DASHES = 51;
	public static final int TYPE_STAGER = 52;
	public static final int TYPE_LISTENING_STAGER_HANDLER = 53;
	
	
	// types that are repeated
	public static final int TYPE_REST = 100;
	public static final int TYPE_STAGELIST_3DASHES = TYPE_REST + TYPE_STAGE_3DASHES;

	private final String name;
	private final int type;
	private final String summary;
	private final boolean optional;

	public Parameter(String name, boolean optional, int type, String summary) {
		this.name = name;
		this.optional = optional;
		this.type = type;
		this.summary = summary;
	}

	public String getName() {
		return name;
	}

	public int getType() {
		return type;
	}

	public String getSummary() {
		return summary;
	}

	public boolean isOptional() {
		return optional;
	}
}
