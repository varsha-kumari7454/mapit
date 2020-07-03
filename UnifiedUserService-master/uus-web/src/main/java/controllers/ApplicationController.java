/**
 * Copyright (C) 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Copyright (C) 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers;

import ninja.Result;
import ninja.Results;

import com.google.inject.Singleton;

@Singleton
public class ApplicationController {

	public Result cors() {
		return Results.json().addHeader("Access-Control-Allow-Origin", "*")
				.addHeader("Access-Control-Allow-Methods", "GET, OPTIONS, HEAD, PUT, POST")
				.addHeader("Access-Control-Allow-Headers",
						"Content-Type,Content-Range, Content-Disposition, Content-Description")
				.addHeader("Access-Control-Allow-Credentials", "true");
	}

	public Result dummy() {
		return Results.html().json().render("Hello world");
	}

	public Result index() {
		return Results.html().template("assets/index.html");
	}
}