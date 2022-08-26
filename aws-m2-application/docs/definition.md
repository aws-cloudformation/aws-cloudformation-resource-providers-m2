# AWS::M2::Application Definition

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#s3location" title="S3Location">S3Location</a>" : <i>String</i>,
    "<a href="#content" title="Content">Content</a>" : <i>String</i>
}
</pre>

### YAML

<pre>
<a href="#s3location" title="S3Location">S3Location</a>: <i>String</i>
<a href="#content" title="Content">Content</a>: <i>String</i>
</pre>

## Properties

#### S3Location

_Required_: Yes

_Type_: String

_Pattern_: <code>^\S{1,2000}$</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Content

_Required_: Yes

_Type_: String

_Minimum_: <code>1</code>

_Maximum_: <code>65000</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

