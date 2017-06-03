#!/usr/bin/env bash

# Deploy Docs only for builds out of master and not PRs or tags.
if ([ "$TRAVIS_BRANCH" == "master" ] || [ ! -z "$TRAVIS_TAG" ]) &&
  [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    git config user.name "Ashwanth Kumar"
    git config user.email "ashwanthkumar@googlemail.com"
    git remote add gh-token "https://${GH_TOKEN}@github.com/ashwanthkumar/suuchi.git";
    git fetch gh-token && git fetch gh-token gh-pages:gh-pages;

    virtualenv --system-site-packages ${HOME}/DENV
    source ${HOME}/DENV/bin/activate
    pip install mkdocs==0.15.3
    mkdocs gh-deploy -v --clean --remote-name gh-token;
fi
