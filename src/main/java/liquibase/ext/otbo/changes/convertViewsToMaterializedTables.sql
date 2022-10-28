declare @schemaName as varchar(max);
declare @viewName as varchar(max);

set @schemaName = schema_name();

declare db_cursor cursor for  
	select v.name from sys.views v where schema_id() = v.schema_id;
open db_cursor 
fetch next from db_cursor into @viewName
while @@FETCH_STATUS = 0
begin
	declare @tableName as varchar(max);
	declare @tableFQN as varchar(max);
	declare @viewFQN as varchar(max);
	declare @sql as varchar(max);
	declare @startTime as DATETIME;
	declare @endTime as DATETIME;
	declare @diff as DATETIME;
	
	set @startTime = CURRENT_TIMESTAMP;
	set @tableName = convert(varchar(max), floor(rand()*9000+1000) ) + 'mv_' + @viewName;
	set @tableFQN = '[' + @schemaName + '].[' + @tableName + ']';
	set @viewFQN = '[' + @schemaName + '].[' + @viewName + ']';
	
	exec('select * into ' + @tableFQN + ' from ' + @viewFQN);
	
	set @endTime = CURRENT_TIMESTAMP;
	set @diff = @endTime - @startTime;
	
	-- Add an extended property to the table so that we have a chance of identifying which tables are 
	-- materializations of views.
	exec sp_addextendedproperty 
		@name = N'MaterializedView', 
		@value = 'True',
		@level0type = N'SCHEMA', @level0name = @schemaName,  
		@level1type = N'TABLE',  @level1name = @tableName;  
	exec sp_addextendedproperty 
		@name = N'StartTime', 
		@value =  @startTime,
		@level0type = N'SCHEMA', @level0name = @schemaName,  
		@level1type = N'TABLE',  @level1name = @tableName;  
	exec sp_addextendedproperty 
		@name = N'EndTime', 
		@value = @endTime,
		@level0type = N'SCHEMA', @level0name = @schemaName,  
		@level1type = N'TABLE',  @level1name = @tableName;  
	exec sp_addextendedproperty 
		@name = N'MaterializationTime', 
		@value = @diff,
		@level0type = N'SCHEMA', @level0name = @schemaName,  
		@level1type = N'TABLE',  @level1name = @tableName;  
	exec('drop view ' + @viewFQN);
	exec sp_rename @objname = @tableFQN, @newname = @viewName;

	fetch next from db_cursor into @viewName
end
close db_cursor;
deallocate db_cursor;
