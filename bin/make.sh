#!/bin/bash
# Build the war artifact and release it to the UKGovLD s3 bucket for use in deployments
# Assumes deployer credentials have been set in the environment via something like:
#    export AWS_ACCESS_KEY_ID=ABC
#    export AWS_SECRET_ACCESS_KEY=DEF
#    export AWS_DEFAULT_REGION=eu-west-1

#mvn clean package || { echo "Maven build failed" 1>&2; exit 1; }
[[ `echo target/*.war` =~ target/(.*)$ ]] || { echo "Can't find war artifact" 1>&2; exit 1; }
upload=${BASH_REMATCH[1]}
echo "Uploading to s3://ukgovld/registry-core/$upload"
aws s3 cp target/$upload s3://ukgovld/registry-core/$upload
