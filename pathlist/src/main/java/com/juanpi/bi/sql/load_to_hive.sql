use ${dbName};
alter table ${tableName} add partition (date="${date}", gu_hash="0") location 'hdfs://nameservice1/user/hadoop/${DataPath}/dw_real_path_list_jobs/date=${date}/gu_hash=0/';
alter table ${tableName} add partition (date="${date}", gu_hash="1") location 'hdfs://nameservice1/user/hadoop/${DataPath}/dw_real_path_list_jobs/date=${date}/gu_hash=1/';
alter table ${tableName} add partition (date="${date}", gu_hash="2") location 'hdfs://nameservice1/user/hadoop/${DataPath}/dw_real_path_list_jobs/date=${date}/gu_hash=2/';
alter table ${tableName} add partition (date="${date}", gu_hash="3") location 'hdfs://nameservice1/user/hadoop/${DataPath}/dw_real_path_list_jobs/date=${date}/gu_hash=3/';
alter table ${tableName} add partition (date="${date}", gu_hash="4") location 'hdfs://nameservice1/user/hadoop/${DataPath}/dw_real_path_list_jobs/date=${date}/gu_hash=4/';
alter table ${tableName} add partition (date="${date}", gu_hash="5") location 'hdfs://nameservice1/user/hadoop/${DataPath}/dw_real_path_list_jobs/date=${date}/gu_hash=5/';
alter table ${tableName} add partition (date="${date}", gu_hash="6") location 'hdfs://nameservice1/user/hadoop/${DataPath}/dw_real_path_list_jobs/date=${date}/gu_hash=6/';
alter table ${tableName} add partition (date="${date}", gu_hash="7") location 'hdfs://nameservice1/user/hadoop/${DataPath}/dw_real_path_list_jobs/date=${date}/gu_hash=7/';
alter table ${tableName} add partition (date="${date}", gu_hash="8") location 'hdfs://nameservice1/user/hadoop/${DataPath}/dw_real_path_list_jobs/date=${date}/gu_hash=8/';
alter table ${tableName} add partition (date="${date}", gu_hash="9") location 'hdfs://nameservice1/user/hadoop/${DataPath}/dw_real_path_list_jobs/date=${date}/gu_hash=9/';
alter table ${tableName} add partition (date="${date}", gu_hash="a") location 'hdfs://nameservice1/user/hadoop/${DataPath}/dw_real_path_list_jobs/date=${date}/gu_hash=a/';
alter table ${tableName} add partition (date="${date}", gu_hash="b") location 'hdfs://nameservice1/user/hadoop/${DataPath}/dw_real_path_list_jobs/date=${date}/gu_hash=b/';
alter table ${tableName} add partition (date="${date}", gu_hash="c") location 'hdfs://nameservice1/user/hadoop/${DataPath}/dw_real_path_list_jobs/date=${date}/gu_hash=c/';
alter table ${tableName} add partition (date="${date}", gu_hash="d") location 'hdfs://nameservice1/user/hadoop/${DataPath}/dw_real_path_list_jobs/date=${date}/gu_hash=d/';
alter table ${tableName} add partition (date="${date}", gu_hash="e") location 'hdfs://nameservice1/user/hadoop/${DataPath}/dw_real_path_list_jobs/date=${date}/gu_hash=e/';
alter table ${tableName} add partition (date="${date}", gu_hash="f") location 'hdfs://nameservice1/user/hadoop/${DataPath}/dw_real_path_list_jobs/date=${date}/gu_hash=f/';

-- alter table dw_path_list_new partition (date="2016-08-31", gu_hash="0") set location 'hdfs://nameservice1/user/hadoop/gongzi/dw_real_path_list/date=2016-08-31/gu_hash=0';