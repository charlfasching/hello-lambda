# Hello Lambda Function

This project contains an AWS Lambda maven application with [AWS Java SDK 2.x](https://github.com/aws/aws-sdk-java-v2) dependencies.

Here follows instructions from AWS Document on building [Lambda for Java](https://docs.aws.amazon.com/lambda/latest/dg/java-image.html#java-image-instructions) 

## Prerequisites
- Java 1.8+
- Apache Maven
- Docker

## Development

### Maven Archetype

The archetype used can be found on Github [here](https://github.com/aws/aws-sdk-java-v2/tree/master/archetypes/archetype-lambda)
```shell
mvn archetype:generate \                                                                                                                                                                                                                                               <aws:charl>
-DarchetypeGroupId=software.amazon.awssdk \
-DarchetypeArtifactId=archetype-lambda \
-DarchetypeVersion=2.29.8 \
-DgroupId=cap.cca.mig \
-DartifactId=hello-lambda \
-Dservice=s3  \
-DinteractiveMode=false
```
We will not be using AWS S3 buckets or files, but the service parameter is mandatory. The examples show s3.

 
The configured AWS Java SDK client is created in `DependencyFactory` class. 
You can add the code in App class to interact with the SDK client based on your use case.

### Lambda 

The default code does inject an S3 Client, but does not do much.
The generated function handler class merely returns back what was given as input.
Seeing as we just wanted a hello world lambda, I removed the S3 client, and kept the hello part.

### Docker

Most of the Dockerfile can be left as is, but some adjustment is need on CMD in order to execute our Lambda Handler.

- The FROM line specifies which Base Image to use. Unless you want to the change the Java runtime, you can leave this untouched.

- The COPY lines are what copies the App code and aws dependencies into the container for later execution.

- The CMD line is what specifies which Java class to call from the Lambda runtime
  - In this case it would be "cap.cca.mig.App::handleRequest"

#### Building the project

#### Maven 
```
mvn compile dependency:copy-dependencies -DincludeScope=runtime
```
The copy-dependency command helps to make the needed aws dependencies available for the Dockerfile to copy    

#### Docker 
Building it locally is simplest for demo purposes. Ideally this should be done by some CICD tool in Business context. 
```
docker build . --platform linux/amd64 -t hello-lambda-java:latest
```
_The build command specifies the --platform linux/amd64 option to ensure that your container is compatible with the Lambda execution environment regardless of the architecture of your build machine. ._


#### Testing it locally

If you use the lambda docker image provided by AWS, it will have the Lambda Interface Emulator included in the Image.
This means it will have a Golang server running on port 8080

All we have to do is start the container with port mapping. 
```
docker run -ti -p 9000:8080 hello-lambda-java:latest
```
This will keep it running in foreground and allow us to make HTTP calls on port 9000.

You may use any HTTP Client to make the call, I included a http request for IntelliJ in the src/test directory.
Here is a curl for reference.
```
$ curl -v "http://localhost:9000/2015-03-31/functions/function/invocations" -d '{"payload":"world!"}'
 
* Host localhost:9000 was resolved.
* IPv6: ::1
* IPv4: 127.0.0.1
*   Trying [::1]:9000...
* Connected to localhost (::1) port 9000
> POST /2015-03-31/functions/function/invocations HTTP/1.1
> Host: localhost:9000
> User-Agent: curl/8.7.1
> Accept: */*
> Content-Length: 20
> Content-Type: application/x-www-form-urlencoded
> 
* upload completely sent off: 20 bytes
< HTTP/1.1 200 OK
< Date: Thu, 21 Nov 2024 16:13:29 GMT
< Content-Length: 14
< Content-Type: text/plain; charset=utf-8
< 
* Connection #0 to host localhost left intact
"hello world!"%                                
```


#### Adding more SDK clients
To add more service clients, you need to add the specific services modules in `pom.xml` and create the clients in `DependencyFactory` following the same 
pattern as s3Client.

## Deployment
The default template included wit the mvn archetype suggests to deploy with SAM, but I don't like it for simple demos where we want to understand the steps.
Feel free to read more below and try it for yourself.

### Supporting AWS Infrastructure
Before we can deploy the Lambda function, we need to set up a few other components it would need.

#### ECR
An Elastic Container Registry (ECR) is needed for a place to push the local docker image to, and also where lambda can read it from.

_Assumption: The aws cli has been installed and configured to connect to the chosen AWS account in the correct region_ 
Tip: Chosen profile can set as Environment variable to be default when running cli command  
```shell
 export AWS_PROFILE=chosen-profile
```

1) Let's start with a simple check, by listing all available ECR repositories
```shell
$ aws ecr describe-repositories
{
    "repository": {
        "repositoryArn": "arn:aws:ecr:eu-west-1:000000000000:repository/test",
        "registryId": "000000000000",
        "repositoryName": "test",
        "repositoryUri": "000000000000.dkr.ecr.eu-west-1.amazonaws.com/test",
        "createdAt": "2024-11-22T14:32:15.890000+01:00",
        "imageTagMutability": "MUTABLE",
        "imageScanningConfiguration": {
            "scanOnPush": false
        },
        "encryptionConfiguration": {
            "encryptionType": "AES256"
        }
    }
}
```
Note: This response would be empty if you have never created a repository in this account and region combination.

2) Now, we want to create a ecr repo for the Lambda
```shell
$ aws ecr create-repository --repository hello-lambda-java
{
    "repositoryArn": "arn:aws:ecr:eu-west-1:000000000000:repository/hello-lambda-java",
    "registryId": "000000000000",
    "repositoryName": "hello-lambda-java",
    "repositoryUri": "000000000000.dkr.ecr.eu-west-1.amazonaws.com/hello-lambda-java",
    "createdAt": "2024-11-08T15:51:23.216000+01:00",
    "imageTagMutability": "MUTABLE",
    "imageScanningConfiguration": {
        "scanOnPush": false
    },
    "encryptionConfiguration": {
        "encryptionType": "AES256"
    }
}
```

3) Verify the repo, it should be empty
```shell
$ aws ecr list-images --repository-name hello-lambda-java
{
    "imageIds": []
}
```

#### IAM
1) Create trust policy  
```shell
cat > trust-policy.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF
```

2) Create the role for Lambda to use
```shell
$ aws iam create-role   --role-name hello-lambda-java    --assume-role-policy-document file://trust-policy.json
{
    "Role": {
        "Path": "/",
        "RoleName": "hello-lambda-java",
        "RoleId": "AROAZVRHILQVXKGB5Z6PD",
        "Arn": "arn:aws:iam::000000000000:role/hello-lambda-java",
        "CreateDate": "2024-11-09T19:03:48+00:00",
        "AssumeRolePolicyDocument": {
            "Version": "2012-10-17",
            "Statement": [
                {
                    "Effect": "Allow",
                    "Principal": {
                        "Service": "lambda.amazonaws.com"
                    },
                    "Action": "sts:AssumeRole"
                }
            ]
        }
    }
}
```

3) Attaching permissions to new Role
```shell

$ aws iam attach-role-policy \
    --role-name hello-lambda-java \
    --policy-arn arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly

$ aws iam attach-role-policy \
    --role-name hello-lambda-java \
    --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

$ aws ecr set-repository-policy \
    --repository-name hello-lambda-java \
    --policy-text '{
        "Version": "2008-10-17",
        "Statement": [
            {
                "Effect": "Allow",
                "Principal": {
                    "Service": "lambda.amazonaws.com"
                },
                "Action": [
                    "ecr:GetDownloadUrlForLayer",
                    "ecr:BatchGetImage",
                    "ecr:BatchCheckLayerAvailability"
                ]
            }
        ]
    }'
```

### Upload Docker
1) Login to ECR
This is a two-step process
- retrieve password from AWS
- do docker login with given password

Tip: It's quite useful to create a small bash script for this, then it saves some time/effort in the future.

```shell
export password=$(aws ecr get-login-password )
docker login --username AWS --password $password 000000000000.dkr.ecr.eu-west-1.amazonaws.com
```
 
2) Prepare Docker image

 To upload the docker image into ECR, it needs to be retagged to the "repositoryUri" from the ecr repo.
```shell
docker tag hello-lambda-java:latest 000000000000.dkr.ecr.eu-west-1.amazonaws.com/hello-lambda-java
``` 
 You can verify the full name of the docker by listing docker images and filtering the result 
```shell
docker images | grep ecr
```
3) Upload
Finally, you may push the image to the ecr 
```shell
docker push $awsAccountId.dkr.ecr.eu-west-1.amazonaws.com/hello-lambda-java
```
If everything was done successfully, the docker image should now be available on ECR 

4) Check ECR Image
```shell
aws ecr list-images --repository-name hello-lambda-java
{
    "imageIds": [
        {
            "imageDigest": "sha256:<some hash value>",
            "imageTag": "latest"
        }
    ]
} 
```
### Lambda Deployment 

1) The time has come to finally create the lambda function
```shell
aws lambda create-function \                                                                                                                                        
    --function-name hello-lambda-java \
    --package-type Image \
    --code ImageUri=000000000000.dkr.ecr.eu-west-1.amazonaws.com/hello-lambda-java:1 \
    --role arn:aws:iam::000000000000:role/hello-lambda-java \
    --region eu-west-1
{
    "FunctionName": "hello-lambda-java",
    "FunctionArn": "arn:aws:lambda:eu-west-1:000000000000:function:hello-lambda-java",
    "Role": "arn:aws:iam::000000000000:role/hello-lambda-java",
    "CodeSize": 0,
    "Description": "",
    "Timeout": 3,
    "MemorySize": 128,
    "LastModified": "2024-11-09T19:13:53.579+0000",
    "CodeSha256": "0a1965fc5d425de9c703ac85468893dfb27b103700f9e935e2d6e7ff716c88eb",
    "Version": "$LATEST",
    "TracingConfig": {
        "Mode": "PassThrough"
    },
    "RevisionId": "bcd74fb3-5f40-4039-8804-de2bbb92f8fb",
    "State": "Pending",
    "StateReason": "The function is being created.",
    "StateReasonCode": "Creating",
    "PackageType": "Image",
    "Architectures": [
        "x86_64"
    ],
    "EphemeralStorage": {
        "Size": 512
    },
    "SnapStart": {
        "ApplyOn": "None",
        "OptimizationStatus": "Off"
    },
    "LoggingConfig": {
        "LogFormat": "Text",
        "LogGroup": "/aws/lambda/hello-lambda-java"
    }
}
```

2) Verify Function 
```shell
aws lambda list-functions                                                                                                                                <aws:charl> 
{
    "Functions": [
        {
            "FunctionName": "hello-lambda-java2",
            "FunctionArn": "arn:aws:lambda:eu-west-1:000000000000:function:hello-lambda-java2",
            "Role": "arn:aws:iam::000000000000:role/hello-lambda-java",
            "CodeSize": 0,
            "Description": "",
            "Timeout": 3,
            "MemorySize": 128,
            "LastModified": "2024-11-11T16:34:41.952+0000",
            "CodeSha256": "0a1965fc5d425de9c703ac85468893dfb27b103700f9e935e2d6e7ff716c88eb",
            "Version": "$LATEST",
            "TracingConfig": {
                "Mode": "PassThrough"
            },
            "RevisionId": "5c8a6e16-3cb7-417e-9fbf-1cff2568e455",
            "PackageType": "Image",
            "Architectures": [
                "x86_64"
            ],
            "EphemeralStorage": {
                "Size": 512
            },
            "SnapStart": {
                "ApplyOn": "None",
                "OptimizationStatus": "Off"
            },
            "LoggingConfig": {
                "LogFormat": "Text",
                "LogGroup": "/aws/lambda/hello-lambda-java2"
            }
        },
        {
            "FunctionName": "hello-lambda-java",
            "FunctionArn": "arn:aws:lambda:eu-west-1:000000000000:function:hello-lambda-java",
            "Role": "arn:aws:iam::000000000000:role/hello-lambda-java",
            "CodeSize": 0,
            "Description": "",
            "Timeout": 3,
            "MemorySize": 128,
            "LastModified": "2024-11-09T19:13:53.579+0000",
            "CodeSha256": "0a1965fc5d425de9c703ac85468893dfb27b103700f9e935e2d6e7ff716c88eb",
            "Version": "$LATEST",
            "TracingConfig": {
                "Mode": "PassThrough"
            },
            "RevisionId": "2ccbec6e-bb87-43fc-b6e8-dc823e5edc0a",
            "PackageType": "Image",
            "Architectures": [
                "x86_64"
            ],
            "EphemeralStorage": {
                "Size": 512
            },
            "SnapStart": {
                "ApplyOn": "None",
                "OptimizationStatus": "Off"
            },
            "LoggingConfig": {
                "LogFormat": "Text",
                "LogGroup": "/aws/lambda/hello-lambda-java"
            }
        }
    ]
}
```

### SAM Deployment (Alternative)

The generated project contains a default [SAM template](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/sam-resource-function.html) file `template.yaml` where you can 
configure different properties of your lambda function such as memory size and timeout. 
You might also need to add specific policies to the lambda function so that it can access other AWS resources.

To deploy the application, you can run the following command:

```
sam deploy --guided
```

See [Deploying Serverless Applications](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-deploying.html) for more info.

## Testing 

### Local Testing

### Remote Testing 

