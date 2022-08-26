# AWS::M2::Application

Represents an application that runs on an AWS Mainframe Modernization Environment

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::M2::Application",
    "Properties" : {
        "<a href="#definition" title="Definition">Definition</a>" : <i><a href="definition.md">Definition</a></i>,
        "<a href="#description" title="Description">Description</a>" : <i>String</i>,
        "<a href="#enginetype" title="EngineType">EngineType</a>" : <i>String</i>,
        "<a href="#name" title="Name">Name</a>" : <i>String</i>,
        "<a href="#tags" title="Tags">Tags</a>" : <i><a href="tags.md">Tags</a></i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::M2::Application
Properties:
    <a href="#definition" title="Definition">Definition</a>: <i><a href="definition.md">Definition</a></i>
    <a href="#description" title="Description">Description</a>: <i>String</i>
    <a href="#enginetype" title="EngineType">EngineType</a>: <i>String</i>
    <a href="#name" title="Name">Name</a>: <i>String</i>
    <a href="#tags" title="Tags">Tags</a>: <i><a href="tags.md">Tags</a></i>
</pre>

## Properties

#### Definition

_Required_: Yes

_Type_: <a href="definition.md">Definition</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Description

_Required_: No

_Type_: String

_Maximum_: <code>500</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### EngineType

_Required_: Yes

_Type_: String

_Allowed Values_: <code>microfocus</code> | <code>bluage</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### Name

_Required_: Yes

_Type_: String

_Pattern_: <code>^[A-Za-z0-9][A-Za-z0-9_\-]{1,59}$</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### Tags

_Required_: No

_Type_: <a href="tags.md">Tags</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the ApplicationArn.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### ApplicationArn

Returns the <code>ApplicationArn</code> value.

#### ApplicationId

Returns the <code>ApplicationId</code> value.

