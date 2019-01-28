export dest_ip=$1

#52.88.245.74

echo "scping $2 to $dest_ip"
scp -i /Users/jeffmc/work/amazon/geodesystems.pem $2 ec2-user@${dest_ip}:


