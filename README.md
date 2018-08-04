# KloudFormation Specification Merger

```kotlin
val template = KloudFormationTemplate.create {
    val topic = topic("NotificationTopic")
    val notificationEmail = parameter<String>("EmailAddress")
    subscription("NotificationSubscription") {
        topicArn(topic.ref())
        protocol("email")
        endpoint(notificationEmail.ref())
    }
}
```

Results in the following Cloud Formation Template

```yaml
---
AWSTemplateFormatVersion: "2010-09-09"
Description: ""
Parameters:
  EmailAddress:
    Type: "String"
Resources:
  NotificationTopic:
    Type: "AWS::SNS::Topic"
  NotificationSubscription:
    Type: "AWS::SNS::Subscription"
    Endpoint:
      Ref: "EmailAddress"
    Protocol: "email"
    TopicArn:
      Ref: "NotificationTopic"
```