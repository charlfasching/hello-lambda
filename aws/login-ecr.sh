#!/bin/bash
password=$(aws ecr get-login-password )
ecrURI=$awsAccountId.dkr.ecr.eu-west-1.amazonaws.com
docker login --username AWS --password $password $ecrURI