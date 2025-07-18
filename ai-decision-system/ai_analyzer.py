import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestRegressor, IsolationForest
from sklearn.preprocessing import StandardScaler
from sklearn.model_selection import train_test_split
from datetime import datetime, timedelta
import joblib
import logging
from config import TELEWORK_THRESHOLDS, RESERVATION_THRESHOLDS, MODEL_CONFIG

logger = logging.getLogger(__name__)

class AIAnalyzer:
    def __init__(self):
        self.telework_thresholds = TELEWORK_THRESHOLDS
        self.reservation_thresholds = RESERVATION_THRESHOLDS
        self.model_config = MODEL_CONFIG
        self.scaler = StandardScaler()
        self.occupancy_model = None
        self.telework_model = None
        self.anomaly_detector = None
        
    def analyze_telework_trends(self, telework_data, user_data):
        """Analyze telework trends and provide recommendations, including daily, weekly, and monthly telework percentages"""
        if telework_data.empty:
            return self._empty_analysis_result("telework")
        
        analysis = {
            'current_percentage': 0,
            'trend': 'stable',
            'recommendations': [],
            'warnings': [],
            'alerts': [],
            'daily_percentages': [],
            'weekly_percentage': 0,
            'monthly_percentage': 0
        }
        
        # Calculate daily telework percentage
        if not user_data.empty:
            total_users = len(user_data)
            telework_data = telework_data.copy()
            telework_data['telework_percentage'] = telework_data['approved_requests'] / total_users * 100
            # List of {date, percentage}
            analysis['daily_percentages'] = [
                {'date': str(row['date']), 'percentage': round(row['telework_percentage'], 2)}
                for _, row in telework_data.iterrows()
            ]
            # Current percentage (last day)
            if not telework_data.empty:
                analysis['current_percentage'] = round(telework_data.iloc[-1]['telework_percentage'], 2)
            else:
                analysis['current_percentage'] = 0
            # Calculate weekly and monthly averages
            telework_data['date'] = pd.to_datetime(telework_data['date'])
            today = pd.Timestamp(datetime.now().date())
            # This week: from Monday to today
            week_start = today - pd.Timedelta(days=today.weekday())
            week_mask = (telework_data['date'] >= week_start) & (telework_data['date'] <= today)
            week_data = telework_data.loc[week_mask]
            if not week_data.empty:
                analysis['weekly_percentage'] = round(week_data['telework_percentage'].mean(), 2)
            else:
                analysis['weekly_percentage'] = 0
            # This month: from 1st to today (include all days, fill missing with 0%)
            telework_data['date'] = pd.to_datetime(telework_data['date'])
            today = pd.Timestamp(datetime.now().date())
            month_start = today.replace(day=1)
            all_days = pd.date_range(start=month_start, end=today, freq='D')
            # Create a Series with all days, fill missing with 0
            telework_pct = telework_data.set_index('date').reindex(all_days, fill_value=0)['telework_percentage']
            if not telework_pct.empty:
                analysis['monthly_percentage'] = round(telework_pct.mean(), 2)
            else:
                analysis['monthly_percentage'] = 0
        
        # Analyze trends (existing logic)
        if len(telework_data) >= 7:
            recent_week = telework_data.tail(7)['approved_requests'].mean()
            previous_week = telework_data.tail(14).head(7)['approved_requests'].mean()
            if recent_week > previous_week * 1.1:
                analysis['trend'] = 'increasing'
            elif recent_week < previous_week * 0.9:
                analysis['trend'] = 'decreasing'
        # Generate recommendations based on thresholds (existing logic)
        if analysis['current_percentage'] > self.telework_thresholds['max_percentage']:
            analysis['alerts'].append({
                'type': 'CRITICAL',
                'message': f"Télétravail critique: {analysis['current_percentage']}% (max: {self.telework_thresholds['max_percentage']}%)",
                'action': 'Réduire les autorisations de télétravail immédiatement'
            })
        elif analysis['current_percentage'] > self.telework_thresholds['warning_threshold']:
            analysis['warnings'].append({
                'type': 'WARNING',
                'message': f"Télétravail élevé: {analysis['current_percentage']}%",
                'action': 'Surveiller et limiter les nouvelles demandes'
            })
        elif analysis['current_percentage'] < self.telework_thresholds['optimal_range'][0]:
            analysis['recommendations'].append({
                'type': 'INFO',
                'message': f"Télétravail faible: {analysis['current_percentage']}%",
                'action': 'Encourager le télétravail pour optimiser l\'espace'
            })
        return analysis
    
    def analyze_reservation_occupancy(self, reservation_data, desk_data):
        """Analyze desk occupancy and provide recommendations"""
        if reservation_data.empty:
            return self._empty_analysis_result("reservation")
        
        analysis = {
            'current_occupancy': 0,
            'average_occupancy': 0,
            'trend': 'stable',
            'recommendations': [],
            'warnings': [],
            'alerts': [],
            'workstation_recommendations': []
        }
        
        # Calculate current occupancy
        if not reservation_data.empty:
            current_occupancy = reservation_data['occupancy_rate'].iloc[-1] if 'occupancy_rate' in reservation_data.columns else 0
            average_occupancy = reservation_data['occupancy_rate'].mean() if 'occupancy_rate' in reservation_data.columns else 0
            
            analysis['current_occupancy'] = round(current_occupancy, 2)
            analysis['average_occupancy'] = round(average_occupancy, 2)
        
        # Analyze trends
        if len(reservation_data) >= 7:
            recent_week = reservation_data.tail(7)['occupancy_rate'].mean()
            previous_week = reservation_data.tail(14).head(7)['occupancy_rate'].mean()
            
            if recent_week > previous_week * 1.05:
                analysis['trend'] = 'increasing'
            elif recent_week < previous_week * 0.95:
                analysis['trend'] = 'decreasing'
        
        # Generate recommendations based on occupancy
        if analysis['current_occupancy'] > self.reservation_thresholds['critical_high']:
            analysis['alerts'].append({
                'type': 'CRITICAL',
                'message': f"Occupation critique: {analysis['current_occupancy']}%",
                'action': 'Ajouter immédiatement des postes de travail'
            })
            analysis['workstation_recommendations'].append({
                'action': 'ADD',
                'quantity': self._calculate_additional_workstations(analysis['current_occupancy']),
                'priority': 'HIGH',
                'reason': 'Occupation critique détectée'
            })
        elif analysis['current_occupancy'] > self.reservation_thresholds['warning_high']:
            analysis['warnings'].append({
                'type': 'WARNING',
                'message': f"Occupation élevée: {analysis['current_occupancy']}%",
                'action': 'Prévoir l\'ajout de postes de travail'
            })
        elif analysis['current_occupancy'] < self.reservation_thresholds['critical_low']:
            analysis['alerts'].append({
                'type': 'CRITICAL',
                'message': f"Occupation très faible: {analysis['current_occupancy']}%",
                'action': 'Envisager la réduction du nombre de postes'
            })
            analysis['workstation_recommendations'].append({
                'action': 'REMOVE',
                'quantity': self._calculate_removable_workstations(analysis['current_occupancy']),
                'priority': 'MEDIUM',
                'reason': 'Occupation faible détectée'
            })
        elif analysis['current_occupancy'] < self.reservation_thresholds['warning_low']:
            analysis['warnings'].append({
                'type': 'WARNING',
                'message': f"Occupation faible: {analysis['current_occupancy']}%",
                'action': 'Analyser l\'utilisation des postes'
            })
        
        return analysis
    
    def predict_future_trends(self, data, days_ahead=30):
        """Predict future trends using machine learning"""
        if data.empty or len(data) < 14:
            return {'predictions': [], 'confidence': 0}
        
        try:
            # Prepare features
            data['date'] = pd.to_datetime(data['date'])
            data['day_of_week'] = data['date'].dt.dayofweek
            data['month'] = data['date'].dt.month
            data['day_of_year'] = data['date'].dt.dayofyear
            
            # Create lag features
            if 'occupancy_rate' in data.columns:
                data['occupancy_lag_1'] = data['occupancy_rate'].shift(1)
                data['occupancy_lag_7'] = data['occupancy_rate'].shift(7)
                target = 'occupancy_rate'
            elif 'telework_percentage' in data.columns:
                data['telework_lag_1'] = data['telework_percentage'].shift(1)
                data['telework_lag_7'] = data['telework_percentage'].shift(7)
                target = 'telework_percentage'
            else:
                return {'predictions': [], 'confidence': 0}
            
            # Remove rows with NaN
            data = data.dropna()
            
            if len(data) < 10:
                return {'predictions': [], 'confidence': 0}
            
            # Prepare features for model
            feature_columns = ['day_of_week', 'month', 'day_of_year']
            if f'{target.split("_")[0]}_lag_1' in data.columns:
                feature_columns.extend([f'{target.split("_")[0]}_lag_1', f'{target.split("_")[0]}_lag_7'])
            
            X = data[feature_columns]
            y = data[target]
            
            # Train model
            model = RandomForestRegressor(n_estimators=100, random_state=42)
            model.fit(X, y)
            
            # Generate future dates
            last_date = data['date'].max()
            future_dates = pd.date_range(start=last_date + timedelta(days=1), periods=days_ahead, freq='D')
            
            # Prepare future features
            future_data = pd.DataFrame({'date': future_dates})
            future_data['day_of_week'] = future_data['date'].dt.dayofweek
            future_data['month'] = future_data['date'].dt.month
            future_data['day_of_year'] = future_data['date'].dt.dayofyear
            
            # Make predictions
            predictions = []
            for i, row in future_data.iterrows():
                features = row[feature_columns].values.reshape(1, -1)
                pred = model.predict(features)[0]
                predictions.append({
                    'date': row['date'].strftime('%Y-%m-%d'),
                    'predicted_value': round(pred, 2)
                })
            
            # Calculate confidence (R² score)
            confidence = model.score(X, y)
            
            return {
                'predictions': predictions,
                'confidence': round(confidence, 3)
            }
            
        except Exception as e:
            logger.error(f"Error in prediction: {e}")
            return {'predictions': [], 'confidence': 0}
    
    def detect_anomalies(self, data, column='occupancy_rate'):
        """Detect anomalies in the data"""
        if data.empty or column not in data.columns:
            return []
        
        try:
            # Prepare data for anomaly detection
            values = data[column].values.reshape(-1, 1)
            
            # Train isolation forest
            iso_forest = IsolationForest(contamination=0.1, random_state=42)
            anomalies = iso_forest.fit_predict(values)
            
            # Get anomalous dates
            anomalous_dates = data[anomalies == -1]['date'].tolist()
            
            return anomalous_dates
            
        except Exception as e:
            logger.error(f"Error in anomaly detection: {e}")
            return []
    
    def generate_comprehensive_report(self, dataset):
        """Generate comprehensive AI analysis report"""
        report = {
            'timestamp': datetime.now().isoformat(),
            'telework_analysis': self.analyze_telework_trends(dataset['telework'], dataset['users']),
            'reservation_analysis': self.analyze_reservation_occupancy(dataset['reservation'], dataset['desks']),
            'predictions': {},
            'anomalies': {},
            'summary': {}
        }
        
        # Add predictions
        if not dataset['reservation'].empty:
            report['predictions']['occupancy'] = self.predict_future_trends(dataset['reservation'])
        
        if not dataset['telework'].empty:
            report['predictions']['telework'] = self.predict_future_trends(dataset['telework'])
        
        # Add anomalies
        if not dataset['reservation'].empty:
            report['anomalies']['occupancy'] = self.detect_anomalies(dataset['reservation'])
        
        if not dataset['telework'].empty:
            report['anomalies']['telework'] = self.detect_anomalies(dataset['telework'], 'telework_percentage')
        
        # Generate summary
        report['summary'] = self._generate_summary(report)
        
        return report
    
    def _calculate_additional_workstations(self, current_occupancy):
        """Calculate how many workstations to add"""
        if current_occupancy > 95:
            return 5
        elif current_occupancy > 90:
            return 3
        elif current_occupancy > 85:
            return 2
        else:
            return 1
    
    def _calculate_removable_workstations(self, current_occupancy):
        """Calculate how many workstations can be removed"""
        if current_occupancy < 30:
            return 3
        elif current_occupancy < 50:
            return 2
        else:
            return 1
    
    def _empty_analysis_result(self, analysis_type):
        """Return empty analysis result"""
        return {
            'current_percentage': 0 if analysis_type == 'telework' else 0,
            'current_occupancy': 0 if analysis_type == 'reservation' else 0,
            'trend': 'stable',
            'recommendations': [],
            'warnings': [],
            'alerts': [],
            'workstation_recommendations': [] if analysis_type == 'reservation' else None
        }
    
    def _generate_summary(self, report):
        """Generate summary of the analysis"""
        summary = {
            'total_alerts': len(report['telework_analysis']['alerts']) + len(report['reservation_analysis']['alerts']),
            'total_warnings': len(report['telework_analysis']['warnings']) + len(report['reservation_analysis']['warnings']),
            'total_recommendations': len(report['telework_analysis']['recommendations']) + len(report['reservation_analysis']['recommendations']),
            'priority_actions': []
        }
        
        # Collect priority actions
        for alert in report['telework_analysis']['alerts'] + report['reservation_analysis']['alerts']:
            summary['priority_actions'].append({
                'priority': 'CRITICAL',
                'action': alert['action'],
                'reason': alert['message']
            })
        
        for warning in report['telework_analysis']['warnings'] + report['reservation_analysis']['warnings']:
            summary['priority_actions'].append({
                'priority': 'WARNING',
                'action': warning['action'],
                'reason': warning['message']
            })
        
        return summary 