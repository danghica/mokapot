Benchmark Suite
===============

The classes in the `benchmarksuite` package and its subpackages are
for collecting performance information on `mokapot` and comparing it
with the performance of RMI, as well as local execution, in a way that
enables regression testing.  Benchmarking is performed with a set of
small "example programs", for each of which there exist a `mokapot`
implementation, an RMI implementation, and a local version.


Instructions for Modification/Extension
---------------------------------------

All the classes are quite thoroughly documented with Javadocs. The
recommended approach is to first read this introductory document, and
then proceed into the code. Once there, one should start by reading
the documentation in this order:

 1. `Main`
 2. `Runner`
 3. `Benchmark`
 4. `BenchmarkImp`
 5. `Benchmarkable`
 6. `RemoteProcess`

In general, adding new benchmark programs is a simple matter of
implementing `Benchmarkable`. Changing what data is collected, or how
it is collected, is a matter of modifying `Runner` and `BenchmarkImp`.


Package Structure
-----------------

The package consists of three sub-packages:

 1. `benchmark` contains all of the code that implements the actual
    benchmarking logic. The most important classes in this package are
    `Main` and `Runner`. The former is the entry point for the
    benchmarking application and contains very little program
    logic. The latter is the most important class and contains the
    majority of the benchmarking algorithm.

 2. `programs` contains all of the small programs that are actually
    used for benchmarking, as well as the `Benchmarkable` interface,
    which has to be implemented by any program that is to be used for
    benchmarking.

 3. `resources` contains html files that are used as templates when
    generating a table containing the collated results of a benchmark
    run.

Flow of Execution
-----------------

The application is launched via `Main`, where an instance of `Runner`
is created.

`Runner` creates a set of `Benchmark` objects, each of which
encapsulates a single `Benchmarkable` program. A `Benchmark` **also
associates with the `Benchmarkable` an arbitrary number of remote
hosts** that it may need. In most cases this would be one remote host,
as most of the example programs are structured to operate in a client-
server architecture (a notable exception being `megamerger`). `Runner`
then calls an execution method on every `Benchmark` object it created.

`Benchmark`'s execution method defines the exact logic by which an
example program is executed and timing information is collected. This
involves calling certain methods defined in the `Benchmarkable`
interface multiple times within loops, and measuring the time the
calls take to return. The method returns a `BenchmarkResult`, which
encapsulates all the metric collected from a single program. `Runner`
uses its `BenchmarkResult` objects to create an HTML file containing
all the data in a table.

`Benchmarkable` defines the structure of programs used for
benchmarking; the core idea is to divide the programs into four
phases: `setup`, `distribute`, `executeAlgorithm`, and
`stop`. `distribute` and `executeAlgorithm` are the two methods we
want to benchmark. The former contains all the logic which establishes
a given network topology by instantiating objects on remote hosts in a
particular distribution. The latter then executes an algorithm using
the distributed objects.

How to Run
----------

You can use the build.xml script for running the benchmark and hosting
the required servers. To run in the default configuration:

 1. Run `ant benchmark-server-mokapot`. Wait until the server is ready.

 2. In another process (in parallel to the process in step 1), run
    `ant benchmark-server-rmi`. Wait until that server is ready.

 3. In another process (in parallel to the previous two processes),
    run `ant benchmark-mokapot`. This will automatically connect to
    the two previous servers, assuming they're all running on
    localhost.

Step 1, running `benchmark-mokapot-server`, will automatically
generate a full default configuration for the Mokapot server, RMI
server, and benchmarking script. (The Mokapot server runs on port
15238, and the RMI server on port 15239; it is probably best to have
these ports firewalled, to reduce any risk of attackers using them to
run arbitrary code on your computer.)

For other benchmarking configurations, you will need to change the
`build-internal/benchmark-config` directory. `mokapot-servers.txt`
and `rmi-servers.txt` hold the lists of servers that the benchmarking
script will use; these contain lines of the form `hostname port`.

`client.p12` and `password.txt` hold the cryptographic material used
to authenticate the Mokapot client to a server. (The Mokapot servers
you run will also need their own `.p12` files, but the benchmarking
interface will not care about this.)

Please ensure you have enough servers to run any of the tests. If a
test requires x servers, the first x servers provided will be chosen.

Once you have a benchmarking configuration set up, run the main method
in `xyz.acygn.mokapot.benchmarksuite.benchmark.Main` with program
arguments specifying the number of distribution and execution
iterations (defaults are 10 and 200 respectively).

The output of the benchmark suite will be placed into
`build-internal/benchmark-output` by default, with two subfolders:
`benchmarks` and `logs`. New benchmarks will be saved in the
`benchmarks` subfolder, any log lines produced during execution will
be saved in the `logs` subfolder.
