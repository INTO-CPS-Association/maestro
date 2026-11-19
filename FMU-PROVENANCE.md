# Test FMU provenance and build investigation

## Current release and coverage

The standalone `INTO-CPS-Association/test-fmus` repository now publishes
`Release/0.0.2`. Maestro's online test-FMU downloads use
`target/online-cache/test-fmus` relative to each test module, including the
FMI 3 reference tests. External CLI/API tests share Maestro's cache. The existing
checked-in FMUs remain available to other tests. The temporary Maven `test-fmus`
module has been removed.

The canonical per-model inventory and current exclusions are in the
[test-fmus catalog](https://github.com/INTO-CPS-Association/test-fmus/blob/master/FMU-CATALOG.md)
and [Maestro coverage section](https://github.com/INTO-CPS-Association/test-fmus/blob/master/README.md#maestro-coverage).
The release is **not a replacement for every Maestro fixture**:

- Controllers that require VDM-RT at runtime, including the ESA, line-follower
  and GA controllers, are intentionally excluded. The three-tank standalone
  controller is replaced by its source-backed C build in `Release/0.0.2`; its
  embedded `.vdmrt` files are documentation/regeneration inputs only.
- `periodic_clock.fmu` and `sinewave_array.fmu` are binary-only Altair exports;
  their model sources were not found.
- The pump and sink mock FMUs are source-less test doubles.
- The source-backed FMI 3 function test is now included in the default release
  with final FMI `3.0` metadata and its expected token.
- The line-follower repository's `3DanimationFMU.fmu` has no bundled sources;
  its VDM controllers are also excluded. Its six plant/sensor FMUs are included.

The test-FMUs release now replaces the source-backed FMI 2 and selected FMI 3
models used by Maestro tests, including the three-tank controller. Resource
configs and MaBL loads use the stable online cache, and their owning tests
declare required release assets before execution. Source-less mocks,
VDM-RT-only controllers, and binary-only FMI 3 exports remain local where
needed. The Jacobian CSV comparison permits floating-point rounding differences
with a `1e-12` absolute and relative tolerance while preserving exact text
comparisons. The release builds and native-load checks do not prove
numerical equivalence or complete FMI conformance; release notes contain the
per-FMU FMPy diagnostic results.

## Declaring test fixtures

The `fmu-test-utils` module owns the `TestFmu` catalog, repository tests, and
the repeatable, inherited JUnit Jupiter `@RequiresFmu` annotation:

```java
@RequiresFmu({TestFmu.SINGLE_WATER_TANK, TestFmu.WATER_TANK_CONTROLLER})
class WaterTankTest {
}
```

The extension downloads declared models before `@BeforeAll` initialization.
Subclasses combine their requirements with those of their superclasses;
duplicate models are downloaded only once. Tests use
`FmuTestModelRepository.cachePath(TestFmu.SINGLE_WATER_TANK)` rather than
string-based download calls. The Maestro-only `TestFmuCollector` handles MaBL
load rewriting; it does not duplicate the shared downloader.

The pinned release URL and canonical asset names are defined once in the
`TestFmu` enum. Java tests use the enum directly. Maven generates
`fmu-test-utils/target/classes/org/intocps/maestro/testutils/test-fmus.properties`
from it during `process-classes`, packages the resource in the utility JAR,
and external Python tests read that generated file. Do not edit or commit the
generated properties; run `mvn -pl fmu-test-utils -am package` before running
external tests separately. A release update changes the enum, not every
configuration path. The cache's `.release` marker causes
old fixtures to be invalidated when the pinned release URL changes. The cache
path remains `target/online-cache/test-fmus`.

## Earlier JNI investigation

The observations below predate the JNI 1.6.1 upgrade and successful four-platform
verification; they are retained as investigation history.

On macOS ARM with the jenv Java 21 JDK, the downloader tests, the FMI 3 reference
simulation, and the FMI 3 builder tests pass. The FMI 2 online tests pass three
of four scenarios; rollback fails at termination with the current JNI snapshot.
Both rollback sources return the same hard-coded component pointer (`(void*) 2`),
while the JNI component registry is keyed by that pointer across FMUs. The second
registration overwrites the first, which is then rejected as invalid. The same
pointer behavior exists in the original model sources; this failure does not
establish that the rebuild changed their behavior. The registry or the FMU
implementations need a separate fix before this scenario can be considered
verified. The Jacobian suite likewise passes seven of eight scenarios; its
`legacy_discard` scenario fails when freeing one of the colliding instances.
The full Maven reactor passes the focused downloader tests with
`-Dmaven.compiler.useIncrementalCompilation=false`; stale incremental output
otherwise caused compilation errors in `modeldefinitionchecker`.

The remainder of this document is the historical investigation and contains
earlier decisions and paths that may no longer describe the current setup.

Investigation date: 2026-11-12. This is a handoff for continuing in another chat.
Repository: `INTO-CPS-Association/maestro`.

## User requirements and current decision

The user dislikes the current, newly added `test-fmus` module's Java-driven native
build and mixed shared include directory. Original requested design:

- Preserve each unpacked FMU's structure, with its source code and a compiling
  `CMakeLists.txt` in `sources/`.
- Build with CMake, not Java code containing per-model source lists.
- Have a top-level CMake project build all models and install packaged FMUs under
  `target`, available as Maven classpath resources and in the installed artifact.
- Invoke compilation during Maven resource processing or compilation.
- Obtain standard FMI headers from `modelica/fmi-standard`, tags `v2.0.5` and
  `v3.0.2`, according to the FMU version, instead of a mixed includes directory.
- Consult https://fmi-standard.org/docs/3.0.2/ for source distribution and build
  descriptions. Do not assume the standard mandates a particular CMake layout;
  verify the relevant section before implementing.

The user then changed direction: **if models already live in external FMU
repositories, do not compile them in Maestro**. They asked first for a complete
provenance overview, before deciding how to restructure or publish artifacts.
No external-repository migration, dependency change, or native-build rewrite has
been implemented. Do not resume the original local CMake rewrite automatically.
No final artifact-consumption mechanism has been selected.

## Current local module

`test-fmus/` is untracked in the current worktree. Relevant files:

- `test-fmus/pom.xml`
- `test-fmus/README.md`
- `test-fmus/build-support/BuildFmus.java`
- `test-fmus/src/main/java/org/intocps/maestro/testfmus/TestFmu.java`
- `test-fmus/src/main/resources/org/intocps/maestro/testfmus/catalog.tsv`
- `test-fmus/src/main/resources/fmus/<version>/<group>/<model>/`
- `test-fmus/src/test/java/org/intocps/maestro/testfmus/TestFmuTest.java`
- `test-fmus/src/test/java/org/intocps/maestro/testfmus/NativeLifecycleTest.java`

The parent POM already has an uncommitted `test-fmus` module entry, alongside
other unrelated dependency/repository edits. Preserve unrelated work.

Currently `BuildFmus.java` runs at `generate-resources`, selects source lists and
compiler definitions in Java, generates CMake projects, compiles, and packages
FMUs. Generated resources go to
`target/generated-resources/org/intocps/maestro/testfmus/<id>.fmu`.
It writes a platform manifest and preserves `target/native-cache` across Maven
clean, using input/toolchain fingerprints. `TestFmu` installs resources to disk
and rejects native platform mismatches. These describe existing behavior, not
an approved future design.

## Repository relationships

| Repository | Default branch | Relevant tip | Findings |
|---|---|---|---|
| https://github.com/INTO-CPS-Association/maestro-test-fmus | `development` | `c0646b7e3cd0c7d8f9769ab8db97bc3968a167bc` | Only branch is development; last commit 2017-11-09 |
| https://github.com/INTO-CPS-Association/test-fmus | `master` | master `4634c0a0c43a63fc72a242b96ae9d51993f7071d` | Also has development at exactly the same `c0646b7...` commit as maestro-test-fmus |
| https://github.com/INTO-CPS-Association/Reference-FMUs | `master` | `f64980512e4709737ab2b47cba2baa3be0dd6767` | Older fork of modelica/Reference-FMUs, last inspected commit from 2020 |
| https://github.com/INTO-CPS-Association/org.intocps.maestro.fmi | inspected `development` | `22a155c28caa08753379ba1e9f29f8db049dcfd4` | JNI fixtures and CMake-generation tooling under jnifmuapi/src/test/resources |
| https://github.com/modelica/Reference-FMUs/tree/v0.0.23 | tag `v0.0.23` | `0a8585a2a3a945f30be8486d90252db9baeceb9c` | Actual source version of the imported BouncingBall and Feedthrough |

`test-fmus/development` and `maestro-test-fmus/development` are identical commits,
not merely similar model directories. Either provides the same legacy sources.
Neither repository is marked as a GitHub fork.

`test-fmus/master` has six model directories with model descriptions:
`diff_pair`, `integrate`, `sine`, `singlewatertank-20sim`, `watertank-c`, and
`watertankcontroller-c`. Its latest commit (2021-09-21) adds `diff_pair`.
Master and development diverged: development has 83 unique commits and master
has 6 unique commits, with merge base `6a1c3cd8efd10800993d9e88702756c65627e00d`.
Consolidation must preserve both histories' unique content.

Creation dates: test-fmus 2018-02-27; maestro-test-fmus 2018-06-21. The latter was
last pushed on its creation date. This suggests a copied development history,
but the reason for creating two repositories is **not established**. The user's
hypothesis that one was for Maestro 1 remains unproven.

### Contributors

Authenticated user is Kenneth Lausdahl (`lausdahl`). All branch histories checked:

- maestro-test-fmus/development: all 84 commits authored by Kenneth.
- test-fmus/development: the same 84 commits authored by Kenneth.
- test-fmus/master: 4 commits authored by Kenneth and 3 by Casper Thule.
- Some commits use GitHub/web-flow as committer; this is not another human author.

### Build and distribution observations

Legacy repositories have per-model CMake projects and `xcompile.sh`, which builds
multiple platforms and packages FMUs. The old script also exports some Overture
models through a tool-wrapper path. Its hard-coded old toolchains/URLs and CMake
minimums should not be assumed usable with current tools without validation.

The INTO-CPS Reference-FMUs fork has a top-level CMake project and an old Azure
pipeline. BouncingBall and Feedthrough exist there, but **not at the same source
version as the local imports**.

No GitHub release assets were returned for maestro-test-fmus, test-fmus, or the
INTO-CPS Reference-FMUs fork. External source presence is not yet a usable pinned
binary-artifact supply. A release/dependency strategy is still required.

## Full catalog: 26 FMUs

22 FMI 2 fixtures and 4 FMI 3 fixtures. Below, archive paths are relative to the
Maestro repository. The local ID is also the suffix under
`test-fmus/src/main/resources/fmus/`.

"External sources" means byte-matching C/C++/header files were found at the
listed location, not that the entire packaged FMU is identical or interchangeable.
Historical earliest authorship was not established.

### Legacy external sources: 10 fixtures

For each linked path, the identical directory also exists in
https://github.com/INTO-CPS-Association/test-fmus/tree/development .

| Enum | Local ID | Type | Imported archive | External source project |
|---|---|---|---|---|
| SINGLE_WATERTANK | fmi2/maestro/singlewatertank-20sim | 20-sim generated native C tank | maestro/src/test/resources/singlewatertank-20sim.fmu | [singlewatertank-20sim](https://github.com/INTO-CPS-Association/maestro-test-fmus/tree/development/singlewatertank-20sim) |
| WATERTANK_CONTROLLER | fmi2/maestro/watertankcontroller-c | C/template tank controller | maestro/src/test/resources/watertankcontroller-c.fmu | [watertankcontroller-c](https://github.com/INTO-CPS-Association/maestro-test-fmus/tree/development/watertankcontroller-c) |
| ROLLBACK_END | fmi2/maestro/rollback-end | C++ rollback fixture | maestro/src/test/resources/rollback-end.fmu | [rollback-end](https://github.com/INTO-CPS-Association/maestro-test-fmus/tree/development/rollback-end) |
| ROLLBACK_TEST | fmi2/maestro/rollback-test | C++ rollback fixture | maestro/src/test/resources/rollback-test.fmu | [rollback-test](https://github.com/INTO-CPS-Association/maestro-test-fmus/tree/development/rollback-test) |
| MASS_SPRING_DAMPER_1 | fmi2/mass-spring-damper/MassSpringDamper1 | 20-sim generated mechanical model | maestro/src/test/resources/mass_spring_damper/MassSpringDamper1.fmu | [MassSpringDamper1](https://github.com/INTO-CPS-Association/maestro-test-fmus/tree/development/MassSpringDamper1) |
| MASS_SPRING_DAMPER_2 | fmi2/mass-spring-damper/MassSpringDamper2 | 20-sim generated mechanical model | maestro/src/test/resources/mass_spring_damper/MassSpringDamper2.fmu | [MassSpringDamper2](https://github.com/INTO-CPS-Association/maestro-test-fmus/tree/development/MassSpringDamper2) |
| PUMP | fmi2/maestro/pump | Dymola 2016 generated native C | maestro/src/test/resources/pump.fmu | [pump](https://github.com/INTO-CPS-Association/maestro-test-fmus/tree/development/pump) |
| SINK | fmi2/maestro/sink | Dymola 2016 generated native C | maestro/src/test/resources/sink.fmu | [sink](https://github.com/INTO-CPS-Association/maestro-test-fmus/tree/development/sink) |
| INITIALIZER_WATERTANK | fmi2/initializer/watertank-c | C/template tank | plugins/initializer/src/test/resources/watertank-c.fmu | [watertank-c](https://github.com/INTO-CPS-Association/maestro-test-fmus/tree/development/watertank-c) |
| INITIALIZER_CONTROLLER | fmi2/initializer/watertankcontroller-c | C/template controller; preserve distinct fixture | plugins/initializer/src/test/resources/watertankcontroller-c.fmu | [watertankcontroller-c](https://github.com/INTO-CPS-Association/maestro-test-fmus/tree/development/watertankcontroller-c) |

Both tank-controller variants have matching external C sources. Metadata and
resources still differ; do not merge them based on source equality alone.

### JNI repository: 2 fixtures

Imported from tag `Release/1.5.0`; source files also match the inspected development
branch. Base path: `jnifmuapi/src/test/resources/fmus/`.

| Enum | Local ID | Type | External source |
|---|---|---|---|
| FMI2_FUNCTION_TEST | fmi2/jni/fmi2functiontest | Native C FMI 2 function/API test, not a physical model | [tag source](https://github.com/INTO-CPS-Association/org.intocps.maestro.fmi/tree/Release/1.5.0/jnifmuapi/src/test/resources/fmus/fmi2/fmi2functiontest), [development source](https://github.com/INTO-CPS-Association/org.intocps.maestro.fmi/tree/development/jnifmuapi/src/test/resources/fmus/fmi2/fmi2functiontest) |
| FMI3_FUNCTION_TEST | fmi3/jni/fmi3functiontest | Native C API test; metadata says **3.0-beta.3**, not final FMI 3.0 | [tag source](https://github.com/INTO-CPS-Association/org.intocps.maestro.fmi/tree/Release/1.5.0/jnifmuapi/src/test/resources/fmus/fmi3/fmi3functiontest), [development source](https://github.com/INTO-CPS-Association/org.intocps.maestro.fmi/tree/development/jnifmuapi/src/test/resources/fmus/fmi3/fmi3functiontest) |

This repository additionally contains
[fmi3/bouncingball](https://github.com/INTO-CPS-Association/org.intocps.maestro.fmi/tree/development/jnifmuapi/src/test/resources/fmus/fmi3/bouncingball),
a separate variant, and
[fmu_compiler_util](https://github.com/INTO-CPS-Association/org.intocps.maestro.fmi/tree/development/jnifmuapi/src/test/resources/fmu_compiler_util)
with `prepare_cmake.py`, CMake templates and platform helpers. It parses FMI build
descriptions. Do not assume its older code is bug-free or directly reusable.

### Official Reference-FMUs: 2 fixtures

| Enum | Local ID | Type | Imported origin / external sources |
|---|---|---|---|
| BOUNCING_BALL | fmi3/reference/BouncingBall | FMI 3.0 native C bouncing ball | Official v0.0.23 release FMU; [BouncingBall](https://github.com/modelica/Reference-FMUs/tree/v0.0.23/BouncingBall) |
| FEEDTHROUGH | fmi3/reference/Feedthrough | FMI 3.0 native C variable-type fixture | Official v0.0.23 release FMU; [Feedthrough](https://github.com/modelica/Reference-FMUs/tree/v0.0.23/Feedthrough) |

Shared source files exactly match [src](https://github.com/modelica/Reference-FMUs/tree/v0.0.23/src)
and [include](https://github.com/modelica/Reference-FMUs/tree/v0.0.23/include).
None of the six local model/runtime C/header files compared per model matched the
older INTO-CPS fork. Use the actual v0.0.23 provenance, not that fork by name alone.

### Overture-origin standalone exports: 3 fixtures

| Enum | Local ID | Original archive | Generation tool |
|---|---|---|---|
| ESA_CONTROLLER | fmi2/esa/watertankController-Standalone | maestro-webapi/src/test/resources/esa/fmus/watertankController-Standalone.fmu | Overture FMI Exporter v0.1.7-SNAPSHOT |
| ROBOT_CONTROLLER | fmi2/line-follow-robot/LFRController_Standalone | maestro/src/test/resources/LineFollowRobot_fmus/LFRController_Standalone.fmu | Overture FMI Exporter v0.2.9-SNAPSHOT |
| THREE_TANK_CONTROLLER | fmi2/three-tank/WaterTankController_Standalone | replaced by `fmi2--three-tank--WaterTankController_Standalone.fmu` from test-fmus `Release/0.0.2` | Overture FMI Exporter v0.2.9-SNAPSHOT; rebuilt from embedded C sources |

No exact corresponding export was identified in the inspected external trees.
The legacy repository contains VDM controller projects (including
`watertankcontroller-overture` and `cascading-watertankcontroller-overture`), but
these were **not established as the exact origins** of the standalone archives.

All three original archives contain seven `resources/*.vdmrt` files. Shared names:
`HardwareInterface`, `System`, `Controller`, `Fmi`, and `World`.
ESA and three-tank additionally contain `ValveActuator` and `LevelSensor`;
the robot additionally contains `RobotSensor` and `RobotServo`.

**Important classification:** The user suggested VDM-RT presence means interpreted
Overture FMUs. These particular archives also contain generated model C files
(`Controller.c`, `System.c`, `Fmu.c`, etc.), native libraries for linux32, linux64,
win32, win64 and darwin64, and no JAR. They therefore appear to be compiled
standalone exports retaining VDM model sources, not interpreter-based exports.
VDM-RT presence alone does not distinguish the execution mode.

The existing local module documents a native freeInstance cleanup crash for
ESA_CONTROLLER; its lifecycle test is excluded, while install/load tests remain.
This is not a request to repair that unrelated ownership defect.

### Other fixtures known only from Maestro archives: 9 fixtures

No exact counterpart found in the four inspected INTO-CPS repositories.

| Enum | Local ID | Type | Imported archive |
|---|---|---|---|
| ROBOT_BODY | fmi2/line-follow-robot/Body_Block | 20-sim generated native C | maestro/src/test/resources/LineFollowRobot_fmus/Body_Block.fmu |
| ROBOT_SENSOR | fmi2/line-follow-robot/Sensor_Block | 20-sim generated native C | maestro/src/test/resources/LineFollowRobot_fmus/Sensor_Block.fmu |
| ROBOT_SENSOR_01 | fmi2/line-follow-robot/Sensor_Block_01 | 20-sim generated native C | maestro/src/test/resources/LineFollowRobot_fmus/Sensor_Block_01.fmu |
| ROBOT_SENSOR_02 | fmi2/line-follow-robot/Sensor_Block_02 | 20-sim generated native C | maestro/src/test/resources/LineFollowRobot_fmus/Sensor_Block_02.fmu |
| ROBOT_EXAMPLE_SENSOR_1 | fmi2/line-follow-robot/LineFollower_Examples_SensorBlock1 | OpenModelica v1.11.0-dev-207-gd19f816 generated C | maestro/src/test/resources/LineFollowRobot_fmus/LineFollower_Examples_SensorBlock1.fmu |
| ROBOT_EXAMPLE_SENSOR_2 | fmi2/line-follow-robot/LineFollower_Examples_SensorBlock2 | OpenModelica v1.11.0-dev-207-gd19f816 generated C | maestro/src/test/resources/LineFollowRobot_fmus/LineFollower_Examples_SensorBlock2.fmu |
| THREE_TANK_1 | fmi2/three-tank/threewatertank1 | 20-sim generated native C | maestro/src/test/resources/three_tank/threewatertank1.fmu |
| THREE_TANK_2 | fmi2/three-tank/threewatertank2 | 20-sim generated native C | maestro/src/test/resources/three_tank/threewatertank2.fmu |
| SIGNAL_GENERATOR | fmi3/siggen-feedthrough/SignalGenerator | FMI 3.0 custom native C, Reference-FMUs-derived | maestro/src/test/resources/fmi3/reference/siggen-feedthrough/SignalGenerator.fmu |

None of these nine archives contains `.vdmrt`, `.vdmpp`, or `.vdmsl` files.
Do not treat legacy `cascading-watertank1-20sim` or `cascading-watertank2-20sim`
as interchangeable with the three-tank fixtures: model-specific source equality
was not found. Generic shared FMI headers do not establish model equivalence.

## Verification method and local-copy modifications

For 22 archive-derived fixtures, local unpacked files were compared to entries in
the catalog's original checked-in FMU ZIPs. All retained files were identical,
except ESA (one changed file) and the two OpenModelica robot example sensors
(three changed files each). These are existing local source-copy repairs.

External comparisons used Git blob SHA-1 hashes for C/C++/header files at the
corresponding paths. Verified exact counts for legacy projects:

- singlewatertank: 21/21; each MassSpringDamper: 23/23.
- pump and sink: 105/105 each; rollback models: 1/1 each.
- initializer tank, both controller source copies: 6/6 each.
- JNI fmi2functiontest: 3/3; JNI fmi3functiontest: 9/9.

Existing support dependencies outside the individual FMU trees:

- `build-support/include`: mixed FMI/reference headers, imported from
  modelica/Reference-FMUs v0.0.23. This is the layout the user wants removed.
- `build-support/openmodelica-d19f816`: missing OpenModelica runtime source files,
  pinned to `d19f8168993148b01716ef183b2877ecd44b006d`.
- `build-support/modelica-3.2.2`: Modelica tables, MAT-file, strings, and zlib code
  from ModelicaStandardLibrary v3.2.2, source archive commit `61c02b1`.
- `build-support/licenses`: imported license documents packaged into the JAR.

The module README documents existing repairs: OpenModelica missing includes,
lazy pool initialization, minimal-runtime duplicate-definition handling, zlib
modern-macOS configuration, ESA GC list traversal, and JNI metadata tokens.
Read the actual files before moving/replacing them. FMI3_FUNCTION_TEST has
special instantiation arguments and intentionally legacy metadata.

## Does this Maestro repository depend on either legacy repository?

A search of tracked files found no explicit reference/URL to maestro-test-fmus
or test-fmus. Existing tests use local FMU archives. Some download fixtures from:

- `maestro/src/test/java/org/intocps/maestro/OnlineTestUtils.java`:
  org.intocps.maestro.fmi release `Release/1.5.0`, asset `test-fmus.zip`.
- `maestro/src/test/java/org/intocps/maestro/fmi3/Fmi3ModuleReferenceFmusTest.java`:
  official Reference-FMUs v0.0.23, asset `Reference-FMUs-0.0.23.zip`.

Models used here but absent from test-fmus/master include MassSpringDamper1/2,
rollback-test/end, pump and sink. All exist in **both development branches**;
there are no models unique to maestro-test-fmus when all branches are considered.
Example consuming configurations:

- maestro-webapi/src/test/resources/maestro2/stabilisation_example/stablemm.json
- maestro/src/test/resources/jacobian_step_builder/legacy_discard/env.json
- maestro/src/test/resources/specifications/full/initialize_fixedstep_unfold_loop/env.json

## Pending decisions / continuation

1. Select the canonical external repository and preserve unique content from
   test-fmus/master if consolidating. Do not infer this decision has been made.
2. Decide whether the 12 fixtures not found externally should be moved there,
   retained as archived inputs, or omitted. No user decision yet.
3. Design pinned, platform-appropriate published FMU artifacts, then let Maestro
   consume/install them without native compilation for externally owned models.
4. Account for distinct metadata/resources, native platform support, old FMI 3
   beta fixture semantics, license notices, and the known ESA cleanup limitation.
5. Only then change the existing test-fmus module/build. No implementation was
   performed during this investigation; this handoff is the only file added.
