package reka.jdbc;

import static reka.api.Path.path;
import static reka.api.content.Contents.booleanValue;
import static reka.api.content.Contents.integer;
import static reka.api.content.Contents.utf8;
import static reka.util.Util.unchecked;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import reka.api.content.types.BooleanContent;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.core.data.memory.MutableMemoryData;

public class JdbcUtil {
	
	public static Data extractSchemas(JdbcConnectionProvider provider) {
		
		MutableData schemas = MutableMemoryData.create();
		
		try (Connection connection = provider.getConnection()) {
			
			DatabaseMetaData meta = connection.getMetaData();
			
			ResultSet dbResult = meta.getCatalogs();
			while (dbResult.next()) {
				String dbRef = dbResult.getString(1);
				String db = dbRef.toLowerCase();
				
				/*
				
				TABLE_CAT String => table catalog (may be null)
				TABLE_SCHEM String => table schema (may be null)
				TABLE_NAME String => table name
				TABLE_TYPE String => table type. Typical types are "TABLE", "VIEW",	"SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
				REMARKS String => explanatory comment on the table
				TYPE_CAT String => the types catalog (may be null)
				TYPE_SCHEM String => the types schema (may be null)
				TYPE_NAME String => type name (may be null)
				SELF_REFERENCING_COL_NAME String => name of the designated "identifier" column of a typed table (may be null)
				REF_GENERATION String => specifies how values in SELF_REFERENCING_COL_NAME are created. Values are "SYSTEM", "USER", "DERIVED". (may be null)
	
				*/
				
				ResultSet tableResult = meta.getTables(dbRef, null, null, null);
				while (tableResult.next()) {
					String tableRef = tableResult.getString("TABLE_NAME");
					String table = tableRef.toLowerCase();
					String type = tableResult.getString("TABLE_TYPE");
					if ("SYSTEM TABLE".equals(type)) {
						continue;
					}
					schemas.put(path(db, table, "type"), utf8(type));
					
					ResultSet columnResult = meta.getColumns(dbRef, null, tableRef, null);
					while (columnResult.next()) {
						
						/*
						 
						TABLE_CAT String => table catalog (may be null)
						TABLE_SCHEM String => table schema (may be null)
						TABLE_NAME String => table name
						COLUMN_NAME String => column name
						DATA_TYPE int => SQL type from java.sql.Types
						TYPE_NAME String => Data source dependent type name, for a UDT the type name is fully qualified
						COLUMN_SIZE int => column size. For char or date types this is the maximum number of characters, for numeric or decimal types this is precision.
						BUFFER_LENGTH is not used.
						DECIMAL_DIGITS int => the number of fractional digits
						NUM_PREC_RADIX int => Radix (typically either 10 or 2)
						NULLABLE int => is NULL allowed.
						columnNoNulls - might not allow NULL values
						columnNullable - definitely allows NULL values
						columnNullableUnknown - nullability unknown
						REMARKS String => comment describing column (may be null)
						COLUMN_DEF String => default value (may be null)
						SQL_DATA_TYPE int => unused
						SQL_DATETIME_SUB int => unused
						CHAR_OCTET_LENGTH int => for char types the maximum number of bytes in the column
						ORDINAL_POSITION int	=> index of column in table (starting at 1)
						IS_NULLABLE String => "NO" means column definitely does not allow NULL values; "YES" means the column might allow NULL values. An empty string means nobody knows.
						SCOPE_CATLOG String => catalog of table that is the scope of a reference attribute (null if DATA_TYPE isn't REF)
						SCOPE_SCHEMA String => schema of table that is the scope of a reference attribute (null if the DATA_TYPE isn't REF)
						SCOPE_TABLE String => table name that this the scope of a reference attribure (null if the DATA_TYPE isn't REF)
						SOURCE_DATA_TYPE short => source type of a distinct type or user-generated Ref type, SQL type from java.sql.Types (null if DATA_TYPE isn't DISTINCT or user-generated REF)
								
						*/
						
						String columnRef = columnResult.getString("COLUMN_NAME");
						String column = columnRef.toLowerCase();
						schemas.put(path(db, table, "columns", column, "type"), utf8(columnResult.getString("TYPE_NAME").toLowerCase()));
						schemas.put(path(db, table, "columns", column, "size"), integer(columnResult.getInt("COLUMN_SIZE")));
						schemas.put(path(db, table, "columns", column, "nullable"), booleanValue(columnResult.getInt("NULLABLE") != 0));
						
						String remark = columnResult.getString("REMARKS");
						if (remark != null && !remark.isEmpty()) {
							schemas.put(path(db, table, "columns", column, "remark"), utf8(remark));
						}
						
						String defaultValue = columnResult.getString("COLUMN_DEF");
						if (defaultValue != null) {
							schemas.put(path(db, table, "columns", column, "default"), utf8(defaultValue));
						}
					}
					
					ResultSet indexResult = meta.getIndexInfo(dbRef, null, tableRef, false, false);
					while (indexResult.next()) {
						
						/*
						TABLE_CAT String => table catalog (may be null)
						TABLE_SCHEM String => table schema (may be null)
						TABLE_NAME String => table name
						NON_UNIQUE boolean => Can index values be non-unique. false when TYPE is tableIndexStatistic
						INDEX_QUALIFIER String => index catalog (may be null); null when TYPE is tableIndexStatistic
						INDEX_NAME String => index name; null when TYPE is tableIndexStatistic
						TYPE short => index type:
						tableIndexStatistic - this identifies table statistics that are returned in conjuction with a table's index descriptions
						tableIndexClustered - this is a clustered index
						tableIndexHashed - this is a hashed index
						tableIndexOther - this is some other style of index
						ORDINAL_POSITION short => column sequence number within index; zero when TYPE is tableIndexStatistic
						COLUMN_NAME String => column name; null when TYPE is tableIndexStatistic
						ASC_OR_DESC String => column sort sequence, "A" => ascending, "D" => descending, may be null if sort sequence is not supported; null when TYPE is tableIndexStatistic
						CARDINALITY int => When TYPE is tableIndexStatistic, then this is the number of rows in the table; otherwise, it is the number of unique values in the index.
						PAGES int => When TYPE is tableIndexStatisic then this is the number of pages used for the table, otherwise it is the number of pages used for the current index.
						FILTER_CONDITION String => Filter condition, if any. (may be null)
						*/
	
						String index = indexResult.getString("INDEX_NAME").toLowerCase();
						String column = indexResult.getString("COLUMN_NAME").toLowerCase();
						if (column != null) {
							schemas.put(path(db, table, "columns", column, "indexes", index, "unique"), booleanValue(!indexResult.getBoolean("NON_UNIQUE")));
						} else {
							schemas.put(path(db, table, "indexes", index, "type"), integer(indexResult.getShort("TYPE")));
						}
						
					}
					
					ResultSet pkResult = meta.getPrimaryKeys(dbRef, null, tableRef);
					while (pkResult.next()) {
						
						/*
						TABLE_CAT String => table catalog (may be null)
						TABLE_SCHEM String => table schema (may be null)
						TABLE_NAME String => table name
						COLUMN_NAME String => column name
						KEY_SEQ short => sequence number within primary key
						PK_NAME String => primary key name (may be null)
						*/
						
						String column = pkResult.getString("COLUMN_NAME").toLowerCase();
						schemas.put(path(db, table, "columns", column, "primary"), BooleanContent.TRUE);
						
					}
					
				}
			}
			
			return schemas;
			
		} catch (SQLException e) {
			throw unchecked(e);
		}
	
	}

}
