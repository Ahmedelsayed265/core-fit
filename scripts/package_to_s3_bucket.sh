#!/bin/bash
set -e

# ---- Variables ----
IMAGE_TAG=${IMAGE_TAG:-latest}
ECR_REGISTRY=${ECR_REGISTRY:-public.ecr.aws/u6h5x3y1}
IMAGE_NAME=${IMAGE_NAME:-corefit/devrepo}
BUCKET_NAME=${BUCKET_NAME:-elasticbeanstalk-eu-west-1-890742564852}
AWS_REGION=${AWS_REGION:-eu-west-1}

# ---- Generate Dockerrun.aws.json ----
cat > Dockerrun.aws.json <<EOL
{
  "AWSEBDockerrunVersion": "1",
  "Image": {
    "Name": "$ECR_REGISTRY/$IMAGE_NAME:$IMAGE_TAG",
    "Update": "true"
  },
  "Ports": [
    { "ContainerPort": "8000" }
  ]
}
EOL

# ---- Zip and upload ----
zip $IMAGE_TAG.zip Dockerrun.aws.json
aws s3 cp $IMAGE_TAG.zip s3://$BUCKET_NAME/$IMAGE_TAG.zip --region $AWS_REGION

echo "✅ Uploaded $IMAGE_TAG.zip to S3 bucket: $BUCKET_NAME"
