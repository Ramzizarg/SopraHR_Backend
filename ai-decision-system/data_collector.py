import pandas as pd
import requests
import pymysql
from datetime import datetime, timedelta
from config import DATABASES, API_ENDPOINTS
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class DataCollector:
    def __init__(self):
        self.databases = DATABASES
        self.api_endpoints = API_ENDPOINTS
        
    def get_db_connection(self, service_name):
        """Create database connection for specific service"""
        try:
            db_config = self.databases.get(service_name)
            if not db_config:
                logger.error(f"No database configuration found for service: {service_name}")
                print(f"No database configuration found for service: {service_name}")
                return None
                
            connection = pymysql.connect(
                host=db_config['host'],
                port=db_config['port'],
                user=db_config['user'],
                password=db_config['password'],
                database=db_config['database'],
                charset='utf8mb4',
                cursorclass=pymysql.cursors.DictCursor
            )
            return connection
        except Exception as e:
            logger.error(f"Database connection failed for {service_name}: {e}")
            print(f"Database connection failed for {service_name}: {e}")
            return None
    
    def fetch_telework_data(self, days=30):
        """Fetch telework data from teletravail database"""
        try:
            connection = self.get_db_connection('teletravail')
            if not connection:
                print("fetch_telework_data: No connection to teletravaildb")
                return pd.DataFrame()
            
            query = """
                SELECT 
                    DATE(created_at) as date,
                    COUNT(*) as total_requests,
                    SUM(CASE WHEN status = 'APPROVED' THEN 1 ELSE 0 END) as approved_requests,
                    SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END) as rejected_requests,
                    SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) as pending_requests
                FROM teletravail_request 
                WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL %s DAY)
                GROUP BY DATE(created_at)
                ORDER BY date
            """
            
            with connection.cursor() as cursor:
                cursor.execute(query, (days,))
                data = cursor.fetchall()
            
            connection.close()
            return pd.DataFrame(data)
            
        except Exception as e:
            logger.error(f"Error fetching telework data: {e}")
            print(f"Error fetching telework data: {e}")
            return pd.DataFrame()
    
    def fetch_reservation_data(self, days=30):
        """Fetch reservation data from reservation database"""
        try:
            connection = self.get_db_connection('reservation')
            if not connection:
                print("fetch_reservation_data: No connection to reservationdb")
                return pd.DataFrame()
            
            query = """
                SELECT 
                    DATE(booking_date) as date,
                    COUNT(*) as total_reservations,
                    COUNT(DISTINCT desk_id) as desks_used,
                    COUNT(DISTINCT user_id) as unique_users,
                    (SELECT COUNT(*) FROM desks) as total_desks
                FROM reservations 
                WHERE booking_date >= DATE_SUB(CURDATE(), INTERVAL %s DAY)
                GROUP BY DATE(booking_date)
                ORDER BY date
            """
            
            with connection.cursor() as cursor:
                cursor.execute(query, (days,))
                data = cursor.fetchall()
            
            connection.close()
            return pd.DataFrame(data)
            
        except Exception as e:
            logger.error(f"Error fetching reservation data: {e}")
            print(f"Error fetching reservation data: {e}")
            return pd.DataFrame()
    
    def fetch_user_data(self):
        """Fetch user data from user database"""
        try:
            connection = self.get_db_connection('user')
            if not connection:
                print("fetch_user_data: No connection to soprahr")
                return pd.DataFrame()
            
            query = """
                SELECT 
                    u.id,
                    u.first_name,
                    u.last_name,
                    u.email,
                    u.team,
                    u.role
                FROM user u
            """
            
            with connection.cursor() as cursor:
                cursor.execute(query)
                data = cursor.fetchall()
            
            connection.close()
            return pd.DataFrame(data)
            
        except Exception as e:
            logger.error(f"Error fetching user data: {e}")
            print(f"Error fetching user data: {e}")
            return pd.DataFrame()
    
    def fetch_workstation_data(self):
        """Fetch workstation data from reservation database"""
        try:
            connection = self.get_db_connection('reservation')
            if not connection:
                print("fetch_workstation_data: No connection to reservationdb")
                return pd.DataFrame()
            
            query = """
                SELECT 
                    d.id,
                    d.left,
                    d.top,
                    d.rotation,
                    d.plan_id,
                    COUNT(r.id) as total_reservations,
                    AVG(CASE WHEN r.booking_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) 
                        THEN 1 ELSE 0 END) as occupancy_rate_30d
                FROM desks d
                LEFT JOIN reservations r ON d.id = r.desk_id
                GROUP BY d.id, d.left, d.top, d.rotation, d.plan_id
            """
            
            with connection.cursor() as cursor:
                cursor.execute(query)
                data = cursor.fetchall()
            
            connection.close()
            return pd.DataFrame(data)
            
        except Exception as e:
            logger.error(f"Error fetching workstation data: {e}")
            print(f"Error fetching workstation data: {e}")
            return pd.DataFrame()
    
    def get_complete_dataset(self, days=30):
        """Get complete dataset for AI analysis"""
        logger.info("Collecting data for AI analysis...")
        
        # Fetch all data from different databases
        telework_df = self.fetch_telework_data(days)
        reservation_df = self.fetch_reservation_data(days)
        user_df = self.fetch_user_data()
        workstation_df = self.fetch_workstation_data()
        
        # Calculate daily metrics
        if not reservation_df.empty and not user_df.empty:
            reservation_df['occupancy_rate'] = (
                reservation_df['desks_used'] / reservation_df['total_desks'] * 100
            )
            reservation_df['user_activity_rate'] = (
                reservation_df['unique_users'] / user_df.shape[0] * 100
            )
        
        if not telework_df.empty and not user_df.empty:
            telework_df['approval_rate'] = (
                telework_df['approved_requests'] / telework_df['total_requests'] * 100
            )
            telework_df['telework_percentage'] = (
                telework_df['approved_requests'] / user_df.shape[0] * 100
            )
        
        return {
            'telework': telework_df,
            'reservation': reservation_df,
            'users': user_df,
            'desks': workstation_df
        }
    
    def get_realtime_data(self):
        """Get real-time data for immediate analysis"""
        today = datetime.now().date()
        
        try:
            # Get today's reservations from reservation database
            reservation_connection = self.get_db_connection('reservation')
            telework_connection = self.get_db_connection('teletravail')
            
            reservation_data = {}
            telework_data = {}
            
            if reservation_connection:
                reservation_query = """
                    SELECT 
                        COUNT(*) as today_reservations,
                        COUNT(DISTINCT desk_id) as desks_used,
                        (SELECT COUNT(*) FROM desks WHERE is_active = 1) as total_desks
                    FROM reservations 
                    WHERE DATE(booking_date) = %s
                """
                
                with reservation_connection.cursor() as cursor:
                    cursor.execute(reservation_query, (today,))
                    reservation_data = cursor.fetchone()
                
                reservation_connection.close()
            
            if telework_connection:
                telework_query = """
                    SELECT 
                        COUNT(*) as today_telework_requests,
                        SUM(CASE WHEN status = 'APPROVED' THEN 1 ELSE 0 END) as approved_today
                    FROM teletravail_request 
                    WHERE DATE(created_at) = %s
                """
                
                with telework_connection.cursor() as cursor:
                    cursor.execute(telework_query, (today,))
                    telework_data = cursor.fetchone()
                
                telework_connection.close()
            
            return {
                'reservation': reservation_data,
                'telework': telework_data,
                'date': today
            }
            
        except Exception as e:
            logger.error(f"Error fetching real-time data: {e}")
            print(f"Error fetching real-time data: {e}")
            return {} 