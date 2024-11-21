# App

This project contains an AWS Lambda maven application with [AWS Java SDK 2.x](https://github.com/aws/aws-sdk-java-v2) dependencies.

## Prerequisites
- Java 1.8+
- Apache Maven
- Docker

## Development

The generated function handler class just returns the input. 
The configured AWS Java SDK client is created in `DependencyFactory` class. 
You can add the code in App class to interact with the SDK client based on your use case.

### Lambda 

The default code does inject an S3 Client, but does not do much.
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

### AWS CLI Deployment 




### SAM Deployment (Alternative)

The generated project contains a default [SAM template](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/sam-resource-function.html) file `template.yaml` where you can 
configure different properties of your lambda function such as memory size and timeout. You might also need to add specific policies to the lambda function
so that it can access other AWS resources.

To deploy the application, you can run the following command:

```
sam deploy --guided
```

See [Deploying Serverless Applications](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-deploying.html) for more info.



