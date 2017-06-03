#!/usr/bin/env bash

if ([ "$TRAVIS_COMMIT_MESSAGE" == "[Do Release]" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]);
then
    echo "Triggering a versioned release of the project"
    # Git configurations while doing the version upgrade and commit
    git config user.name "Ashwanth Kumar"
    git config user.email "ashwanthkumar@googlemail.com"
    git remote remove origin
    git remote add origin "https://${GH_TOKEN}@github.com/ashwanthkumar/suuchi.git";

    if [ ! -z "$TRAVIS" -a -f "$HOME/.gnupg" ]; then
        shred -v ~/.gnupg/*
        rm -rf ~/.gnupg
    fi
    source .travis/gpg.sh

    git checkout ${TRAVIS_BRANCH} # we always do a release out of the intended branch
    mvn release:clean release:prepare release:perform --settings .travis/settings.xml -DskipTests=true -DperformRelease --batch-mode --update-snapshots
    echo "Versioned release of the project is now complete"
else
    echo "Triggering a SNAPSHOT release of the project"
    mvn deploy --settings .travis/settings.xml -DskipTests=true -B
    echo "SNAPSHOT release of the project is now complete"
fi
