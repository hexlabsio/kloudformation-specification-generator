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
            role("ROLE"){
                this.policies(kotlin.arrayOf(
                        Policy(
                                policyDocument {
                                    statement(
                                            action("sns:*"),
                                            resources(+"bob", topic.ref())
                                    ) {
                                        principal(PrincipalType.AWS, topic.ref())
                                        principal(PrincipalType.FEDERATED, +"xyz")
                                        condition(ConditionOperators.stringEquals, ConditionKeys.awsUserName, listOf("brian"))
                                    }
                                },
                                policyName = +"NAME"
                        )
                ))
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
    Properties: {}
  NotificationSubscription:
    Type: "AWS::SNS::Subscription"
    Properties:
      Endpoint:
        Ref: "EmailAddress"
      Protocol: "email"
      TopicArn:
        Ref: "NotificationTopic"
  ROLE:
    Type: "AWS::IAM::Role"
    Properties:
      Policies:
      - PolicyDocument:
          Statement:
          - Effect: "Allow"
            Action:
            - "sns:*"
            Resource:
            - "bob"
            - Ref: "NotificationTopic"
            Principal:
              AWS:
                Ref: "NotificationTopic"
              Federated: "xyz"
            Condition:
              StringEquals:
                aws:username:
                - "brian"
        PolicyName: "NAME"
```