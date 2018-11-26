# Checkout

```
git clone git@github.com:INTO-CPS-Association/maestro.git
```

# Protocol
https://github.com/INTO-CPS-Association/maestro/blob/development/orchestration/coe/src/main/resources/coe-protocol/coe-protocol.pdf

# Development Environment

## General

* Install JDK 1.7 (64 bit / optionally 32 bit)
* Install maven 3.x
* Install Eclipse luna + 
 * m2e
 * scala ide - http://download.scala-ide.org/sdk/lithium/e44/scala211/stable/site
 * Maven Integration for Scala IDE - http://alchim31.free.fr/m2e-scala/update-site/m2eclipse-scala/
 
If the Discover connector doesnt discover the apropiate connectors during maven import then please install the connector `m2e connector for build-helper-maven-plugin`

# Development

The coe uses scala and there for it is recomented to use Eclipse Standard with the Scala IDE installed (remember to run the `Scala->Run Setup Diagnistics` otherwise it mostlikely will crash)

Bafore import use a terminal to execute this command in the root of the repository:

```
mvn package
```
this will download all dependencies and execute all the goals excluded in Eclipse.


Import the projects unsing Maven Integeration into Eclipse (m2e): Import->Existing Maven Project, only import the tree leafs

## Repository to download Jenkins builds
http://overture.au.dk:8081/artifactory/into-cps/org/into-cps/orchestration/coe/


# Release procedure

Remain on the development branch.

## Prerequisites
* Python 3 with module requests
* Create a github token with repo full access 
* Create a gpg key to sign with

## Update release notes

If there is no future milestone, then create one.

Go through the issues and set them to a future milestone, if they have not been fixed and are related to the current milestone.

Go to github and close the milestone which should be released.

Then build the release notes:

```bash
cd releaseNotes
./github-fetch-milestone-issues.py 
git add ReleaseNotes* && git commit -m "updated release notes"
cd ..
git push
```

and to update the public issue page https://twt-gmbh.github.io/INTO-CPS-COE/:

```bash
cd docs
./github-fetch-milestone-issues.py
git add index.md && git commit -m "updated issue list"
cd ..
git push
```

## Release the tool
Replace ${RELEASE_VER} with the release (e.g. 0.2.20) and ${NEW_DEV_VER} with dev version (e.g. 0.2.21-SNAPSHOT).
Press enter when prompted for SCM
```bash
mvn -Dmaven.repo.local=repository release:clean
mvn -Dmaven.repo.local=repository release:prepare -DreleaseVersion=${RELEASE_VER} -DdevelopmentVersion=${NEW_DEV_VER}
mvn -Dmaven.repo.local=repository release:perform
```

Now go to the master branch, merge with the newly created tag, and push. REMEMBER TO GO BACK TO DEVELOPMENT BRANCH!

for more see https://github.com/overturetool/overture/wiki/Release-Process

* Sonatype link: https://oss.sonatype.org/#stagingRepositories

## Upload the release

remember to create github release from the released tag and upload: `target/checkout/orchestration/coe/target/coe-0.0.2-jar-with-dependencies.jar`.

Furthermore, remember to update the download.json file in the development branch of INTO-CPS/github.io
