# kloud-formation


## Create a new  Template

```kotlin
val template = KloudFormationTemplate.create {}
```

## Parameters

```kotlin
val template = KloudFormationTemplate.create {
    val email = parameter<String>("EmailAddress")
}
// Result:
// AWSTemplateFormatVersion: "2010-09-09"
// Parameters:
//   EmailAddress:
//     Type: "String"
```

## Pseudo Parameters

Pseudo parameters can be used anywhere a reference can be. Here is the full list of available pseudo parameters:
They exist within the `KloudFormationTemplate.Builder` type and are available within the `create` braces

```kotlin
val awsAccountId = Reference<String>("AWS::AccountId")
val awsNotificationArns = Reference<List<String>>("AWS::NotificationARNs")
fun <T> awsNoValue() = Reference<T>("AWS::NoValue")
val awsPartition = Reference<String>("AWS::Partition")
val awsRegion = Reference<String>("AWS::Region")
val awsStackId = Reference<String>("AWS::StackId")
val awsStackName = Reference<String>("AWS::StackName")
val awsUrlSuffix = Reference<String>("AWS::URLSuffix")
```

## Resources

```kotlin
val template = KloudFormationTemplate.create {
    topic()
}
// Result:
// AWSTemplateFormatVersion: "2010-09-09"
// Resources:
//   Topic:
//     Type: "AWS::SNS::Topic"
```

## References

To get a reference to any resource or parameter simply invoke the `ref()` function

```kotlin
val template = KloudFormationTemplate.create {
    val email = parameter<String>("EmailAddress")
    val topic = topic()
    subscription {
        topicArn(topic.ref())
        endpoint(email.ref())
        protocol("email")
    }
}
// Result:
// AWSTemplateFormatVersion: "2010-09-09"
// Parameters:
//   EmailAddress:
//     Type: "String"
// Resources:
//   Topic:
//     Type: "AWS::SNS::Topic"
//   Subscription:
//     Type: "AWS::SNS::Subscription"
//     Properties:
//       Endpoint:
//         Ref: "EmailAddress"
//       Protocol: "email"
//       TopicArn:
//         Ref: "Topic"
```

## Complex Resources
Required properties appear in the resources building function itself, like cidrBlock in the following example. 
Other properties can be set in the builder function passed to resource builder and are all exposed as functions themselves. (see enableDnsHostnames)

```kotlin
 val template = KloudFormationTemplate.create{
    vPC(cidrBlock = +"0.0.0.0/0"){
        enableDnsHostnames(true)
    }
}
// Result:
// AWSTemplateFormatVersion: "2010-09-09"
// Resources:
//   VPC:
//     Type: "AWS::EC2::VPC"
//     Properties:
//       CidrBlock: "0.0.0.0/0"
//       EnableDnsHostnames: true
```


## Attributes

Each class representing an AWS Resource contains a list of available attributes.
Attributes can be used anywhere a reference can be used or a primitive type.
The following example shows how a bucket could be named with the name of a topic

```kotlin
val template = KloudFormationTemplate.create{
    val topic = topic()
    bucket { 
        bucketName(topic.TopicName())
    }
}
// Result:
// AWSTemplateFormatVersion: "2010-09-09"
// Description: ""
// Resources:
//   Topic:
//     Type: "AWS::SNS::Topic"
//   Bucket:
//     Type: "AWS::S3::Bucket"
//     Properties:
//       BucketName:
//         Fn::GetAtt:
//         - "Topic"
//         - "TopicName"
```

## Joins

Where primitive types, like String are required the actual value can either be the primitive type itself, a reference, an attribute or a join.
Each type can be combined using the plus operator to create a join. The following example is the same as the above example except it adds the string `Bucket` to the value returned from getting the topics name at runtime.

```kotlin
val template = KloudFormationTemplate.create{
    val topic = topic()
    bucket { 
        bucketName(topic.TopicName() + "Bucket")
    }
}
// Result:
// AWSTemplateFormatVersion: "2010-09-09"
// Resources:
//   Topic:
//     Type: "AWS::SNS::Topic"
//   Bucket:
//     Type: "AWS::S3::Bucket"
//     Properties:
//       BucketName:
//         Fn::Join:
//         - ""
//         - - Fn::GetAtt:
//             - "Topic"
//             - "TopicName"
//           - "Bucket"
```

## IAM Policy Documents

AWS simply state that certain resources are of the type JsonNode. To remedy this we have wrapped that in our Value type so that our PolicyDocument type can be used instead.
PolicyDocument lets you build fully type safe iam policies as shown in the example below. 

```kotlin
  val template = KloudFormationTemplate.create{
     val topic = topic(logicalName = "NotificationTopic") // Provide custom logical names
     role(
             assumeRolePolicyDocument = policyDocument {
                 statement(action("sts:AssumeRole")) {
                     principal(PrincipalType.SERVICE, +"lambda.amazonaws.com")
                 }
             }
     ){
         policies(arrayOf(
                 Policy(
                         policyDocument {
                             statement(
                                     action("sns:*"),
                                     IamPolicyEffect.Allow,
                                     resource(topic.ref())
                             ) {
                                 principal(PrincipalType.AWS, +"Some AWS Principal")
                                 condition(ConditionOperators.stringEquals, ConditionKeys.awsUserName, listOf("brian"))
                             }
                         },
                         policyName = +"LambdaSnsPolicy"
                 )
         ))
     }
 }
// Result:
// AWSTemplateFormatVersion: "2010-09-09"
// Resources:
//   NotificationTopic:
//     Type: "AWS::SNS::Topic"
//   Role:
//     Type: "AWS::IAM::Role"
//     Properties:
//       AssumeRolePolicyDocument:
//         Statement:
//         - Effect: "Allow"
//           Action:
//           - "sts:AssumeRole"
//           Principal:
//             Service: "lambda.amazonaws.com"
//       Policies:
//       - PolicyDocument:
//           Statement:
//           - Effect: "Allow"
//             Action:
//             - "sns:*"
//             Resource:
//             - Ref: "NotificationTopic"
//             Principal:
//               AWS: "Some AWS Principal"
//             Condition:
//               StringEquals:
//                 aws:username:
//                 - "brian"
//         PolicyName: "LambdaSnsPolicy"
```

## Alternative Resource References

```kotlin
val template = KloudFormationTemplate.create {
    topic().also { myTopic ->
        subscription {
            topicArn(myTopic.ref())
        }
    }
}
```

## Dependencies

Each builder function takes a `dependsOn` parameter that can be used to set a dependency as follows

```kotlin
val topic = topic()
val q = queue(dependsOn = listOf(topic.logicalName))
```

Alternatively you can chain resources using the `then` function as follows

```kotlin
topic().then{ queue() }
```

You can also combine resources for multi dependant relationships as follows

```kotlin
 (topic() and topic()).then{ topic()  }
 
 // Result
 // AWSTemplateFormatVersion: "2010-09-09"
 // Resources:
 //   Topic:
 //     Type: "AWS::SNS::Topic"
 //   Topic2:
 //     Type: "AWS::SNS::Topic"
 //   Topic3:
 //     Type: "AWS::SNS::Topic"
 //     DependsOn:
 //     - "Topic"
 //     - "Topic2"
```


## Mappings

There is a function exposed when inside the `create` braces called `mappings` that allows you to create the mappings section within your template.
It takes a three level nested map. The following example shows how you might create an ec2 instance based on the region your stack is being created in.

_Note here that the `awsRegion` is one of the Pseudo Parameters (see above for full list)_

```kotlin
val template = KloudFormationTemplate.create {
    val regions = "RegionMap"
    val usEast1 = "us-east-1"
    val euWest1 = "eu-west-1"
    val bits32 = "32"
    mappings(
        regions to mapOf(
                usEast1 to mapOf( bits32 to +"ami-6411e20d"),
                euWest1 to mapOf( bits32 to +"ami-37c2f643")
        )
    )
    instance{
        instanceType("m1.small")
        imageId(FindInMap(+regions, awsRegion, +bits32))
    }
}

// Result
// AWSTemplateFormatVersion: "2010-09-09"
// Mappings:
//   RegionMap:
//     us-east-1:
//       32: "ami-6411e20d"
//     eu-west-1:
//       32: "ami-37c2f643"
// Resources:
//   Instance:
//     Type: "AWS::EC2::Instance"
//     Properties:
//       ImageId:
//         Fn::FindInMap:
//         - "RegionMap"
//         - Ref: "AWS::Region"
//         - "32"
//       InstanceType: "m1.small"
```