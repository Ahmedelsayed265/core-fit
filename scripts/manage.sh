#!/bin/bash
set -e

# 🌍 Environment
export AWS_PAGER=""
AWS_REGION="eu-west-1"

# 🖥️ Resources
EC2_INSTANCE_ID="i-0a5c652a6c9b50498"
RDS_INSTANCE_ID="corefit-dev-db"

# ⏱️ Intervals
EC2_CHECK_INTERVAL=10
RDS_CHECK_INTERVAL=50
TIMEOUT=900

# 📝 Logging
log() {
  echo "[$1] $(date '+%Y-%m-%d %H:%M:%S') - $2"
}

# 🔍 Show resource statuses
show_status() {
  log "STATUS" "Fetching current resource statuses... 🔎"

  ec2_status=$(aws ec2 describe-instances --instance-ids "$EC2_INSTANCE_ID" --region "$AWS_REGION" \
                 --query "Reservations[0].Instances[0].State.Name" --output text 2>/dev/null || echo "unknown")

  rds_status=$(aws rds describe-db-instances --db-instance-identifier "$RDS_INSTANCE_ID" --region "$AWS_REGION" \
                 --query "DBInstances[0].DBInstanceStatus" --output text 2>/dev/null || echo "unknown")

  echo ""
  echo "===== 📡 CURRENT STATUS ====="
  echo "🖥️  EC2 ($EC2_INSTANCE_ID): $ec2_status"
  echo "🛢️  RDS ($RDS_INSTANCE_ID): $rds_status"
  echo "=============================="
  echo ""
}

# 💰 Fetch current AWS bill
get_current_bill() {
  log "BILL" "Fetching current month's bill... 💵"

  START_DATE=$(date +"%Y-%m-01")
  END_DATE=$(date +"%Y-%m-%d")

  result=$(aws ce get-cost-and-usage \
    --time-period Start=$START_DATE,End=$END_DATE \
    --granularity MONTHLY \
    --metrics "UnblendedCost" \
    --group-by Type=DIMENSION,Key=SERVICE \
    --region "$AWS_REGION" 2>/dev/null)

  if [ $? -ne 0 ]; then
    log "BILL" "Cost Explorer is not enabled or permissions missing ❌"
    exit 1
  fi

  total=$(echo "$result" | jq -r '.ResultsByTime[0].Total.UnblendedCost.Amount')
  currency=$(echo "$result" | jq -r '.ResultsByTime[0].Total.UnblendedCost.Unit')

  echo ""
  echo "===== 💵 CURRENT AWS BILL ====="
  echo "📅 Period: $START_DATE → $END_DATE"
  echo "💰 Total: $total $currency"
  echo ""
  echo "📌 Breakdown by Service:"
  echo "$result" | jq -r '.ResultsByTime[0].Groups[] | "• \(.Keys[0]): \(.Metrics.UnblendedCost.Amount) \(.Metrics.UnblendedCost.Unit)"'
  echo "=============================="
  echo ""
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
      status=$(aws ec2 describe-instances --instance-ids "$identifier" --region "$AWS_REGION" \
                --query "Reservations[0].Instances[0].State.Name" --output text)
    elif [ "$resource_type" == "rds" ]; then
      status=$(aws rds describe-db-instances --db-instance-identifier "$identifier" --region "$AWS_REGION" \
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

if [[ "$ACTION" != "start" && "$ACTION" != "stop" && "$ACTION" != "status" && "$ACTION" != "bills" ]]; then
  echo "Usage: $0 [start|stop|status|bills]"
  exit 1
fi

# 📌 BILLS COMMAND
if [ "$ACTION" == "bills" ]; then
  get_current_bill
  exit 0
fi

# 📌 STATUS COMMAND
if [ "$ACTION" == "status" ]; then
  show_status
  exit 0
fi

# 📌 STOP COMMAND
if [ "$ACTION" == "stop" ]; then
  log "STOP" "Stopping resources... 🛑"
  log "STOP" "Stopping EC2 instance $EC2_INSTANCE_ID..."
  aws ec2 stop-instances --instance-ids "$EC2_INSTANCE_ID" --region "$AWS_REGION"
  wait_for_status "STOP" "ec2" "$EC2_INSTANCE_ID" "stopped" $EC2_CHECK_INTERVAL

  log "STOP" "Stopping RDS instance $RDS_INSTANCE_ID..."
  { aws rds stop-db-instance --db-instance-identifier "$RDS_INSTANCE_ID" --region "$AWS_REGION"; } \
    || log "STOP" "RDS not in available state, continuing to wait..."

  wait_for_status "STOP" "rds" "$RDS_INSTANCE_ID" "stopped" $RDS_CHECK_INTERVAL

  log "STOP" "All resources stopped successfully ✅"
fi

# 📌 START COMMAND
if [ "$ACTION" == "start" ]; then
  log "START" "Starting resources... 🚀"

  log "START" "Starting RDS instance $RDS_INSTANCE_ID..."
  { aws rds start-db-instance --db-instance-identifier "$RDS_INSTANCE_ID" --region "$AWS_REGION"; } \
    || log "START" "RDS not in startable state, continuing to wait..."

  wait_for_status "START" "rds" "$RDS_INSTANCE_ID" "available" $RDS_CHECK_INTERVAL

  log "START" "Starting EC2 instance $EC2_INSTANCE_ID..."
  aws ec2 start-instances --instance-ids "$EC2_INSTANCE_ID" --region "$AWS_REGION"
  wait_for_status "START" "ec2" "$EC2_INSTANCE_ID" "running" $EC2_CHECK_INTERVAL

  log "START" "All resources started successfully ✅"
fi
