# Release Workflow
In Suuchi and it's related modules we use the following mechanism of doing releases to sonatype.

## Steps to make a release
1. Make sure you've write access to the repository.
2. Run the `make-release.sh` from the root of the project. 
3. It would create an empty commit with the message `"[Do Release]"`.
4. This commit message would trigger the release workflow using the build tool to build and publish the artifacts to sonatype, which later would get mirrored to maven central.

## Release Process

![Release Workflow](/images/developer/release_workflow.png)
