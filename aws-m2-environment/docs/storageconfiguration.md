# AWS::M2::Environment StorageConfiguration

Defines the storage configuration for an environment.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#efs" title="Efs">Efs</a>" : <i><a href="efsstorageconfiguration.md">EfsStorageConfiguration</a></i>,
    "<a href="#fsx" title="Fsx">Fsx</a>" : <i><a href="fsxstorageconfiguration.md">FsxStorageConfiguration</a></i>
}
</pre>

### YAML

<pre>
<a href="#efs" title="Efs">Efs</a>: <i><a href="efsstorageconfiguration.md">EfsStorageConfiguration</a></i>
<a href="#fsx" title="Fsx">Fsx</a>: <i><a href="fsxstorageconfiguration.md">FsxStorageConfiguration</a></i>
</pre>

## Properties

#### Efs

Defines the storage configuration for an Amazon EFS file system.

_Required_: Yes

_Type_: <a href="efsstorageconfiguration.md">EfsStorageConfiguration</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Fsx

Defines the storage configuration for an Amazon FSx file system.

_Required_: Yes

_Type_: <a href="fsxstorageconfiguration.md">FsxStorageConfiguration</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

