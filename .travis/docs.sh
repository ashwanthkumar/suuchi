#!/usr/bin/env bash

# Deploy Docs only for builds out of master and not PRs or tags.
if ([ "$TRAVIS_BRANCH" == "master" ] || [ ! -z "$TRAVIS_TAG" ]) &&
  [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    git config user.name "Ashwanth Kumar"
    git config user.email "ashwanthkumar@googlemail.com"
    git remote add gh-token "https://${GH_TOKEN}@github.com/ashwanthkumar/suuchi.git";
    git fetch gh-token && git fetch gh-token gh-pages:gh-pages;

    VIRTUAL_ENV=${HOME}/DENV
    mkdir -p $VIRTUAL_ENV

    export PATH=${VIRTUAL_ENV}/python-deps/bin:$PATH
    source ${HOME}/DENV/bin/activate
    pip install --prefix=$VIRTUAL_ENV mkdocs==0.15.3

    export PYTHONPATH=${VIRTUAL_ENV}/lib64/python2.7/site-packages:$PYTHONPATH
    export PATH=${PYTHONPATH}/bin:${PATH}

    ls -lah $VIRTUAL_ENV

    mkdocs gh-deploy -v --clean --remote-name gh-token;
fi
