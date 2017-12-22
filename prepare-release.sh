#!/bin/bash

cd releaseNotes
./github-fetch-milestone-issues.py 
git add ReleaseNotes* && git commit -m "updated release notes"
cd ..


cd docs
./github-fetch-milestone-issues.py
git add index.md && git commit -m "updated issue list"
cd ..
