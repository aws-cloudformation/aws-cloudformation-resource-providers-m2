# AWS::M2::Application

This package contains the handlers used to provision and manage **AWS::M2::Application** as a CloudFormation resource.

The M2 Application is [modelled](https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-model.html)
with a standard schema that defines the resource, its properties, and their attributes in a uniform way.

The [CloudFormation CLI](https://github.com/aws-cloudformation/cloudformation-cli) 
tool is used for validating the schema and for testing and submitting the resource to CloudFormation. 

See more on [developing resource handlers](https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-develop.html).

Main components:
1. The JSON schema describing the resource: `aws-m2-application.json`.
2. Resource handlers:
   
**CreateHandler.java** -  CloudFormation invokes this handler when the resource is initially created during stack create operations.

**ReadHandler.java** - CloudFormation invokes this handler as part of a stack update operation when detailed information about the resource's current state is required.

**UpdateHandler.java** - CloudFormation invokes this handler when the resource is updated as part of a stack update operation.

**ListHandler.java** - CloudFormation invokes this handler when summary information about multiple resources of this resource type is required.

**DeteleHandler.java** - CloudFormation invokes this handler when the resource is deleted, either when the resource is deleted from the stack as part of a stack update operation, or the stack itself is deleted.


The RPDK will automatically generate the correct resource model from the schema whenever the project is built via Maven. You can also do this manually with the following command: `cfn generate`.

> Please don't modify files under `target/generated-sources/rpdk`, as they will be automatically overwritten.

The code uses [Lombok](https://projectlombok.org/), and [you may have to install IDE integrations](https://projectlombok.org/setup/overview) to enable auto-complete for Lombok-annotated classes.
