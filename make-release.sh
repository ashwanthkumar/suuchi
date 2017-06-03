#!/usr/bin/env bash

git stash --all
# We create a special type of commit message to trigger a release workflow in travis-ci
git commit --allow-empty -m "[Do Release]"
git push
git stash pop
