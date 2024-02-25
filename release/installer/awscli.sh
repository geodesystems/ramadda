#!/bin/bash

############################################################################################################
#This script uses the Amazon Command Line Interface (CLI) to create and configure an AWS instance for RAMADDA
#Install the CLI from:
#http://aws.amazon.com/cli/
#Note: the first time you run the CLI you will need to provide user credentials
##these can be set up at:
##https://console.aws.amazon.com/iam/home#/home
############################################################################################################


downloadUrl="https://geodesystems.com/repository/release/latest/ramaddainstaller.zip"
securityGroup="ramadda"
imageId="ami-55a7ea65"
instanceType="t2.micro"
keyPair="ramadda"
volumeSize="100"


usage() {
##    echo "run with no args to create and setup an instance"
##    echo "or if you have already created an instance run:"
    echo "aws.sh <EC2 instance id> <optional .pem file>"
    echo "Make sure your pem file is readable only by you"    
    exit
}


if [ "$1" = "-help" ]; then
    usage
    exit
fi


comment() {
    local msg="$1"
    if [ "$msg" != "" ]; then
        printf "\n# $msg\n"
    fi
}
readit() {
    local msg="$1"
    local var="$2"
    local extra="$3"
    comment "$extra";
    read -p "$msg" $var
}


if [ ! -f ~/.aws/credentials ]; then
    printf "Enter your Amazon authentication  - only needed one time\n"
    aws configure
fi



createInstance() {
    echo "This will create a new EC2 instance then install RAMADDA on it"
    readit  "Enter the image id [${imageId}]: " tmp "Enter machine info. Note: the installer only works on Amazon Linux AMIs"

    if [ "$tmp" != "" ]; then
        imageId=$tmp
    fi
    
    read -p "Enter the instance type [${instanceType}]: " tmp
    if [ "$tmp" != "" ]; then
        instanceType=$tmp
    fi

    read -p "Enter the size (GB) of the storage volume. 0 for none. [${volumeSize}]: " tmp
    if [ "$tmp" != "" ]; then
        volumeSize=$tmp
    fi



    readit  "Enter the key pair name [${keyPair}]: " tmp "The <key pair>.pem file is the way you access your instance."
    if [ "$tmp" != "" ]; then
        keyPair=$tmp
    fi

    if [ -f ${keyPair}.pem ]; then
        echo "Key pair file ${keyPair}.pem already exists"
    else 
        read -p "Do you want to create the keypair file ${keyPair}.pem [y|n]: " tmp
        case $tmp in
            ""|"y")
                aws ec2 create-key-pair --query 'KeyMaterial' --output text --key-name ${keyPair} >  ${keyPair}.pem
                chmod 400 ${keyPair}.pem
                echo "Created the key pair file ${keyPair}.pem"
                echo "Important: This is the only way to access your instance and must be kept private."
                ;;
        esac
    fi



    readit "Security group [${securityGroup}|n]? " tmp "RAMADDA needs ports 22 (ssh), 80 (http) and 443 (https) defined in its security group\nEnter 'n' if you have already created a ${securityGroup} security group"

    case $tmp in
        "n")
            ;;
        *)
            if [ "$tmp" != "" ]; then
                securityGroup="$tmp"
            fi
            aws ec2 create-security-group --group-name ${securityGroup} --description "RAMADDA security group" > /dev/null 2> /dev/null
            aws ec2 authorize-security-group-ingress --group-name ${securityGroup}  --protocol tcp --port 22 --cidr 0.0.0.0/0 > /dev/null 2> /dev/null
            aws ec2 authorize-security-group-ingress --group-name ${securityGroup}  --protocol tcp --port 80 --cidr 0.0.0.0/0 > /dev/null 2> /dev/null
            aws ec2 authorize-security-group-ingress --group-name ${securityGroup}  --protocol tcp --port 443 --cidr 0.0.0.0/0 > /dev/null 2> /dev/null
            aws ec2 authorize-security-group-ingress --group-name ${securityGroup}  --protocol tcp --port 21 --cidr 0.0.0.0/0 > /dev/null 2> /dev/null
            aws ec2 authorize-security-group-ingress --group-name ${securityGroup}  --protocol tcp --port 44001-44099 --cidr 0.0.0.0/0 > /dev/null 2> /dev/null
            ;;
    esac

    read -p  "Set instance name to: " instanceName

    ipAddress=""

    echo "Do you want to create the instance with:\n\timage: ${imageId}\n\ttype: ${instanceType}\n\tsecurity group: ${securityGroup}\n\tInstance name: ${instanceName}"
    read -p "Enter [y|n]: " tmp
    case $tmp in
        ""|"y")
            echo "Creating instance ${instanceName} ... ";
            if [ ${volumeSize} == 0 ]; then
                device="[]"
            else
                device="[{\"DeviceName\":\"/dev/xvdb\",\"Ebs\":{\"VolumeSize\":${volumeSize},\"DeleteOnTermination\":false}}]"
            fi
            aws ec2 run-instances  --output text --image-id ${imageId} --count 1 --instance-type ${instanceType} --key-name ${keyPair} --security-groups ${securityGroup}  --block-device-mappings $device > runinstance.txt
            ;;
        *)
            if [ ! -f runinstance.txt ]; then
                tmp="aws ec2 run-instances --image-id ${imageId} --count 1 --instance-type ${instanceType} --key-name ${keyPair} --security-groups ${securityGroup}  --block-device-mappings \"[{\"DeviceName\":\"/dev/sdf\",\"Ebs\":{\"VolumeSize\":${volumeSize},\"DeleteOnTermination\":false}}]\" "
                printf "We would have called:\n$tmp\n\n"
                exit
            fi
            echo "OK, we will use the instance id from the last run"
            ;; esac


    grep INSTANCES runinstance.txt  | awk 'BEGIN { FS = "\t" } ; { print $8 }'  > instanceid.txt
    instanceId=$( cat instanceid.txt )
    if [ "$instanceId" == "" ]; then
        echo "Failed to read instanceid from runinstance.txt"
        exit
    fi

}



newInstance=1;
if [ "$1" != "" ]; then
    newInstance=0;
    instanceId=$1;
    PEMFILE="${keyPair}.pem"
    if [ "$2" != "" ]; then
        PEMFILE=$2;
    fi
else
    usage
    createInstance
    PEMFILE="${keyPair}.pem"
fi


echo "Instance id: $instanceId"
while [ 1  ]; do
    echo "Waiting for instance to come up..."
    aws ec2 describe-instances --output text --query "Reservations[0].Instances[0].PublicIpAddress" --instance-id "$instanceId"  > ipaddress.txt
    ipAddress=$( cat ipaddress.txt )
    if [ "$ipAddress" == "None" ]; then
        ipAddress="";
    fi
    if [ "$ipAddress" == "ebs" ]; then
        ipAddress="";
    fi
    if [ "$ipAddress" != "" ]; then
        break
    fi
    echo "Not ready yet..."
    sleep 2;
done
if [ "$ipAddress" == "" ]; then
    echo "Failed to read IP address from ipaddress.txt"
    exit
fi



if [ $newInstance ]; then
    printf "Your instance will be ready to access in a minute or two. You will be able to access it at:\n   ssh -i ${keyPair}.pem ec2-user@${ipAddress}\n\n"
    if [ "$instanceName" != "" ]; then
        aws ec2 create-tags --resources ${instanceId} --tags Key=Name,Value=$instanceName
    fi
fi




while [ ! -f $PEMFILE ]; do
    PEMFILE="~/${keyPair}.pem"
    if [ ! -f $PEMFILE ]; then
        read -p "Enter path to ${keyPair}.pem file: " tmp
        if [ "$tmp" == "" ]; then
            exit
        fi
        PEMFILE=$tmp
        if [ ! -f $PEMFILE ]; then
            echo "File doesn't exist: $PEMFILE"
            exit
        fi
    fi
done

echo "We'll keep trying to ssh to the instance and update the OS "
echo "This may take some time while the instance is coming up so have patience "
echo "Once you are connected you will see a 'The authenticity of host ...' message. Enter 'yes' and then the update will run"
echo "trying: ssh -i ${PEMFILE} -t  ec2-user@${ipAddress} \"sudo yum update -y\" "
keepGoing=1;
while [ $keepGoing == 1  ]; do
    result=`ssh   -i ${PEMFILE} -t  ec2-user@${ipAddress} "pwd -y" 2> /dev/null`
    case ${result} in
        "") 
            echo "Instance isn't ready yet. We'll sleep a bit and then try again";
            sleep 10;
            ;;
        *) 
#Now do the update
            ssh   -i ${PEMFILE} -t  ec2-user@${ipAddress} "sudo yum update -y" 
            echo "Instance is ready and updated"
            keepGoing=0;
            ;;
    esac
done


        
readit  "Download and install RAMADDA? [y|n]: " tmp  "OK, we will now ssh to the new instance, download and run the RAMADDA installer"
case $tmp in
    ""|"y")
	echo "Downloading the installer from ${downloadUrl}"
        ssh  -i ${PEMFILE} -t  ec2-user@${ipAddress} "wget -O ramaddainstaller.zip ${downloadUrl}"
        ssh  -i ${PEMFILE} -t  ec2-user@${ipAddress} "unzip -o ramaddainstaller.zip"
        ssh  -i ${PEMFILE} -t  ec2-user@${ipAddress} "sudo sh /home/ec2-user/ramaddainstaller/installer.sh; sleep 5;"
        ;;
esac



printf "Access your instance via:   ssh -i ${PEMFILE} ec2-user@${ipAddress}\n"


