# AWS::M2::Environment

Represents a runtime environment that can run migrated mainframe applications.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::M2::Environment",
    "Properties" : {
        "<a href="#description" title="Description">Description</a>" : <i>String</i>,
        "<a href="#enginetype" title="EngineType">EngineType</a>" : <i>String</i>,
        "<a href="#engineversion" title="EngineVersion">EngineVersion</a>" : <i>String</i>,
        "<a href="#highavailabilityconfig" title="HighAvailabilityConfig">HighAvailabilityConfig</a>" : <i><a href="highavailabilityconfig.md">HighAvailabilityConfig</a></i>,
        "<a href="#instancetype" title="InstanceType">InstanceType</a>" : <i>String</i>,
        "<a href="#name" title="Name">Name</a>" : <i>String</i>,
        "<a href="#preferredmaintenancewindow" title="PreferredMaintenanceWindow">PreferredMaintenanceWindow</a>" : <i>String</i>,
        "<a href="#publiclyaccessible" title="PubliclyAccessible">PubliclyAccessible</a>" : <i>Boolean</i>,
        "<a href="#securitygroupids" title="SecurityGroupIds">SecurityGroupIds</a>" : <i>[ String, ... ]</i>,
        "<a href="#storageconfigurations" title="StorageConfigurations">StorageConfigurations</a>" : <i>[ <a href="storageconfiguration.md">StorageConfiguration</a>, ... ]</i>,
        "<a href="#subnetids" title="SubnetIds">SubnetIds</a>" : <i>[ String, ... ]</i>,
        "<a href="#tags" title="Tags">Tags</a>" : <i><a href="tags.md">Tags</a></i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::M2::Environment
Properties:
    <a href="#description" title="Description">Description</a>: <i>String</i>
    <a href="#enginetype" title="EngineType">EngineType</a>: <i>String</i>
    <a href="#engineversion" title="EngineVersion">EngineVersion</a>: <i>String</i>
    <a href="#highavailabilityconfig" title="HighAvailabilityConfig">HighAvailabilityConfig</a>: <i><a href="highavailabilityconfig.md">HighAvailabilityConfig</a></i>
    <a href="#instancetype" title="InstanceType">InstanceType</a>: <i>String</i>
    <a href="#name" title="Name">Name</a>: <i>String</i>
    <a href="#preferredmaintenancewindow" title="PreferredMaintenanceWindow">PreferredMaintenanceWindow</a>: <i>String</i>
    <a href="#publiclyaccessible" title="PubliclyAccessible">PubliclyAccessible</a>: <i>Boolean</i>
    <a href="#securitygroupids" title="SecurityGroupIds">SecurityGroupIds</a>: <i>
      - String</i>
    <a href="#storageconfigurations" title="StorageConfigurations">StorageConfigurations</a>: <i>
      - <a href="storageconfiguration.md">StorageConfiguration</a></i>
    <a href="#subnetids" title="SubnetIds">SubnetIds</a>: <i>
      - String</i>
    <a href="#tags" title="Tags">Tags</a>: <i><a href="tags.md">Tags</a></i>
</pre>

## Properties

#### Description

The description of the environment.

_Required_: No

_Type_: String

_Maximum_: <code>500</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### EngineType

The target platform for the environment.

_Required_: Yes

_Type_: String

_Allowed Values_: <code>microfocus</code> | <code>bluage</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### EngineVersion

The version of the runtime engine for the environment.

_Required_: No

_Type_: String

_Pattern_: <code>^\S{1,10}$</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### HighAvailabilityConfig

Defines the details of a high availability configuration.

_Required_: No

_Type_: <a href="highavailabilityconfig.md">HighAvailabilityConfig</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### InstanceType

The type of instance underlying the environment.

_Required_: Yes

_Type_: String

_Pattern_: <code>^\S{1,20}$</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Name

The name of the environment.

_Required_: Yes

_Type_: String

_Pattern_: <code>^[A-Za-z0-9][A-Za-z0-9_\-]{1,59}$</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### PreferredMaintenanceWindow

Configures a desired maintenance window for the environment. If you do not provide a value, a random system-generated value will be assigned.

_Required_: No

_Type_: String

_Pattern_: <code>^\S{1,50}$</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### PubliclyAccessible

Specifies whether the environment is publicly accessible.

_Required_: No

_Type_: Boolean

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### SecurityGroupIds

The list of security groups for the VPC associated with this environment.

_Required_: No

_Type_: List of String

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### StorageConfigurations

The storage configurations defined for the runtime environment.

_Required_: No

_Type_: List of <a href="storageconfiguration.md">StorageConfiguration</a>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### SubnetIds

The unique identifiers of the subnets assigned to this runtime environment.

_Required_: No

_Type_: List of String

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### Tags

Defines tags associated to an environment.

_Required_: No

_Type_: <a href="tags.md">Tags</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the EnvironmentArn.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### EnvironmentArn

The Amazon Resource Name (ARN) of the runtime environment.

#### EnvironmentId

The unique identifier of the environment.

