#!/usr/bin/env bash
- git config user.name "Ashwanth Kumar"
- git config user.email "ashwanthkumar@googlemail.com"
- git remote add gh-token "https://${GH_TOKEN}@github.com/ashwanthkumar/suuchi.git";
- git fetch gh-token && git fetch gh-token gh-pages:gh-pages;
- sudo pip install mkdocs==0.15.3
- mkdocs gh-deploy -v --clean --remote-name gh-token;
