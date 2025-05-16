/*
 * Copyright 2023-2025 The STARS Project Authors
 * SPDX-License-Identifier: Apache-2.0
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

plugins { id("tools.aqua.stars.library-conventions") }

mavenMetadata {
  name.set("STARS Data AV")
  description.set(
      "STARS - Scenario-Based Testing of Autonomous Robotic Systems - Data Model for Autonomous Vehicles")
}

dependencies {
  implementation(project(":stars-core"))
  implementation(project(":stars-logic-mtcq"))
  implementation(files("../stars-logic-mtcq/lib/openllet-distribution-2.6.6-SNAPSHOT.jar")) // Topllet
  implementation("org.antlr:antlr4-runtime:4.13.1") // required by Topllet for parsing MTCQs
  implementation("com.googlecode.lanterna:lanterna:3.1.2") // required by Topllet (streaming version)
  implementation(kotlin("reflect"))
  testImplementation(project(":stars-logic-kcmftbl"))
}

configurations { create("test") }

tasks.register<Jar>("testArchive") {
  archiveBaseName.set("tools.aqua.stars.data.av.test")
  from(project.the<SourceSetContainer>()["test"].output)
}

artifacts { add("test", tasks["testArchive"]) }
