#!/usr/bin/env bash

if ([ "$TRAVIS_COMMIT_MESSAGE" == "[Do Release]" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]);
then
    echo "Triggering a versioned release of the project"
    echo "Attempting to publish signed jars"
    sbt +publishSigned
    echo "Published the signed jars"
    echo "Attempting to make a release of the sonatype staging"
    sbt sonatypeRelease
    echo "Released the sonatype staging setup"
    sbt release with-defaults
    echo "Versioned release of the project is now complete"
else
    echo "Triggering a SNAPSHOT release of the project"
    sbt +publish
    echo "SNAPSHOT release of the project is now complete"
fi
