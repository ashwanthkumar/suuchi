[![Build Status](https://travis-ci.org/ashwanthkumar/suuchi.svg?branch=master)](https://travis-ci.org/ashwanthkumar/suuchi)
[![codecov](https://codecov.io/gh/ashwanthkumar/suuchi/branch/master/graph/badge.svg)](https://codecov.io/gh/ashwanthkumar/suuchi)

# Suuchi - सूचि

Having inspired from tools like [Uber's Ringpop](https://ringpop.readthedocs.io/) and a strong desire to understand how distributed systems work - Suuchi was born.

Suuchi is toolkit to build distributed data systems, that uses [gRPC](http://www.grpc.io/) under the hood as the communication medium. The overall goal of this project is to build pluggable components that can be easily composed by the developer to build a data system of desired characteristics.

> This project is in beta quality and it's currently running couple of systems in production setting [@indix](https://twitter.com/indix). We welcome all kinds of feedback to help improve the library.

Read the Documentation at [http://ashwanthkumar.github.io/suuchi](http://ashwanthkumar.github.io/suuchi).

Suuchi in sanskrit means an Index<sup>[1](http://spokensanskrit.de/index.php?tinput=sUci&direction=SE&script=HK&link=yes&beginning=0)</sup>.

## Presentations
Following presentations / videos explain motivation behind Suuchi

- Video by [@brewkode](https://twitter.com/brewkode) on [Suuchi - Toolkit to build distributed systems](https://www.youtube.com/watch?v=GK0-ICFvIGw) at Fifth Elephant, 2017.
- [Suuchi - Distributed Systems Primitives](https://speakerdeck.com/ashwanthkumar/suuchi-distributed-system-primitives)
- [Suuchi - Application Layer Sharding](https://speakerdeck.com/ashwanthkumar/suuchi-application-layer-sharding)
- [Suuchi - Distributed Data Systems Toolkit](https://speakerdeck.com/ashwanthkumar/suuchi-distributed-data-systems-toolkit/)

## Notes
If you're getting `ClassNotFound` exception, please run `mvn clean compile` once to generate from the java classes from protoc files. Also, if you're using IntelliJ it helps to close the project when running the above command. It seems to auto-detect sources in `target/` at startup but not afterwards. 

## Release workflow
Suuchi and it's modules follow a git commit message based release workflow. Use the script `make-release.sh` to push an empty commit to the repository which would trigger a release workflow on travis-ci. More information can be found at [docs](https://ashwanthkumar.github.io/suuchi/developer/workflow/).

## License
https://www.apache.org/licenses/LICENSE-2.0
