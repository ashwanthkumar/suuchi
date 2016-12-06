[![Build Status](https://snap-ci.com/ashwanthkumar/suuchi/branch/master/build_image)](https://snap-ci.com/ashwanthkumar/suuchi/branch/master)
[![codecov](https://codecov.io/gh/ashwanthkumar/suuchi/branch/master/graph/badge.svg)](https://codecov.io/gh/ashwanthkumar/suuchi)

# Suuchi - सूचि

Having inspired from tools like [Uber's Ringpop](https://ringpop.readthedocs.io/) and a strong desire to understand how distributed systems work - Suuchi was born.

Suuchi is toolkit to build distributed data systems, that uses [gRPC](http://www.grpc.io/) under the hood as the communication medium. The overall goal of this project is to build pluggable components that can be easily composed by the developer to build a data system of desired characteristics.

> This project is alpha quality and not meant to be used in any production setting. We welcome all kinds of feedback to help improve the library.

Read the Documentation at [http://ashwanthkumar.github.io/suuchi](http://ashwanthkumar.github.io/suuchi).

Suuchi in sanskrit means an Index<sup>[1](http://spokensanskrit.de/index.php?tinput=sUci&direction=SE&script=HK&link=yes&beginning=0)</sup>.

## Notes
If you're getting `ClassNotFound` exception, please run `mvn clean compile` once to generate from the java classes from protoc files. Also, if you're using IntelliJ it helps to close the project when running the above command. It seems to auto-detect sources in `target/` at startup but not afterwards. 

## License
https://www.apache.org/licenses/LICENSE-2.0
