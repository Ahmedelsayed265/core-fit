#!/bin/bash
set -e

# 🌍 Environment
export AWS_PAGER=""
AWS_REGION="eu-west-1"

# 🖥️ Resources
EC2_INSTANCE_ID="i-0a5c652a6c9b50498"
RDS_INSTANCE_ID="corefit-dev-db"

# ⏱️ Intervals
EC2_CHECK_INTERVAL=10     # ثواني بين كل فحص للـ EC2
RDS_CHECK_INTERVAL=50     # ثواني بين كل فحص للـ RDS
TIMEOUT=900               # timeout كحد أقصى بالثواني لكل resource

# 📝 Logging
log() {
  echo "[$1] $(date '+%Y-%m-%d %H:%M:%S') - $2"
}

# 🔄 Wait for resource status
wait_for_status() {
  local action=$1
  local resource_type=$2
  local identifier=$3
  local desired_status=$4
  local check_interval=$5
  local start_time=$(date +%s)

  while true; do
    if [ "$resource_type" == "ec2" ]; then
      status=$(aws ec2 describe-instances --instance-ids $identifier --region $AWS_REGION \
                --query "Reservations[0].Instances[0].State.Name" --output text)
    elif [ "$resource_type" == "rds" ]; then
      status=$(aws rds describe-db-instances --db-instance-identifier $identifier --region $AWS_REGION \
                --query "DBInstances[0].DBInstanceStatus" --output text 2>/dev/null || echo "not-available")
    else
      log "ERROR" "Unknown resource type: $resource_type"
      exit 1
    fi

    log "$action" "$resource_type $identifier current status: $status"

    if [ "$status" == "$desired_status" ] || [ "$status" == "not-available" ]; then
      log "$action" "$resource_type $identifier reached status $desired_status ✅"
      break
    fi

    # Timeout check
    elapsed=$(( $(date +%s) - start_time ))
    if [ $elapsed -ge $TIMEOUT ]; then
      log "ERROR" "Timeout waiting for $resource_type $identifier to reach $desired_status ❌"
      exit 1
    fi

    sleep $check_interval
  done
}

# 🏁 Main
ACTION=$1

if [[ "$ACTION" != "start" && "$ACTION" != "stop" ]]; then
  echo "Usage: $0 [start|stop]"
  exit 1
fi

if [ "$ACTION" == "stop" ]; then
  log "STOP" "Stopping resources... 🛑"

  # 1️⃣ Stop EC2
  log "STOP" "Stopping EC2 instance $EC2_INSTANCE_ID..."
  aws ec2 stop-instances --instance-ids $EC2_INSTANCE_ID --region $AWS_REGION
  wait_for_status "STOP" "ec2" $EC2_INSTANCE_ID "stopped" $EC2_CHECK_INTERVAL

  # 2️⃣ Stop RDS
  log "STOP" "Stopping RDS instance $RDS_INSTANCE_ID..."
  { aws rds stop-db-instance --db-instance-identifier $RDS_INSTANCE_ID --region $AWS_REGION; } \
    || log "STOP" "RDS $RDS_INSTANCE_ID not in available state, continuing to wait..."
  wait_for_status "STOP" "rds" $RDS_INSTANCE_ID "stopped" $RDS_CHECK_INTERVAL

  log "STOP" "All resources stopped successfully ✅"

elif [ "$ACTION" == "start" ]; then
  log "START" "Starting resources... 🚀"

  # 1️⃣ Start RDS first
  log "START" "Starting RDS instance $RDS_INSTANCE_ID..."
  { aws rds start-db-instance --db-instance-identifier $RDS_INSTANCE_ID --region $AWS_REGION; } \
    || log "START" "RDS $RDS_INSTANCE_ID not in startable state, continuing to wait..."
  wait_for_status "START" "rds" $RDS_INSTANCE_ID "available" $RDS_CHECK_INTERVAL

  # 2️⃣ Start EC2
  log "START" "Starting EC2 instance $EC2_INSTANCE_ID..."
  aws ec2 start-instances --instance-ids $EC2_INSTANCE_ID --region $AWS_REGION
  wait_for_status "START" "ec2" $EC2_INSTANCE_ID "running" $EC2_CHECK_INTERVAL

  log "START" "All resources started successfully ✅"
fi
