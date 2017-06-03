#!/usr/bin/env bash

if [ "${TRAVIS_COMMIT_MESSAGE}" == "[Do Release]" ];
then
    # Git configurations while doing the version upgrade and commit
    git config user.name "Ashwanth Kumar"
    git config user.email "ashwanthkumar@googlemail.com"
    git remote remove origin
    git remote add origin "https://${GH_TOKEN}@github.com/ashwanthkumar/suuchi.git";

    mvn --settings .travis/settings.xml org.codehaus.mojo:versions-maven-plugin:2.1:set -DnewVersion=${TRAVIS_TAG} 1>/dev/null 2>/dev/null

    if [ ! -z "$TRAVIS" -a -f "$HOME/.gnupg" ]; then
        shred -v ~/.gnupg/*
        rm -rf ~/.gnupg
    fi
    source .travis/gpg.sh

    mvn release:perform --settings .travis/settings.xml -DskipTests=true -DperformRelease --batch-mode --update-snapshots
else
    mvn deploy --settings .travis/settings.xml -DskipTests=true -B
fi
