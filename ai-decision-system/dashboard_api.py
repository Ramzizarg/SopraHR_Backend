from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
import uvicorn
import schedule
import time
import threading
from datetime import datetime, timedelta
import json
import logging

from data_collector import DataCollector
from ai_analyzer import AIAnalyzer
from config import MODEL_CONFIG

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Initialize FastAPI app
app = FastAPI(
    title="Workstation AI Decision System",
    description="AI-powered decision support system for workstation management",
    version="1.0.0"
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialize components
data_collector = DataCollector()
ai_analyzer = AIAnalyzer()

# Global cache for analysis results
analysis_cache = {}
last_analysis_time = None

@app.on_event("startup")
async def startup_event():
    """Initialize the system on startup"""
    logger.info("Starting AI Decision System...")
    # Start background task for periodic analysis
    threading.Thread(target=run_scheduled_analysis, daemon=True).start()

def run_scheduled_analysis():
    """Run periodic analysis in background"""
    schedule.every(6).hours.do(perform_analysis)  # Every 6 hours
    schedule.every().day.at("08:00").do(perform_analysis)  # Daily at 8 AM
    
    while True:
        schedule.run_pending()
        time.sleep(60)

def perform_analysis():
    """Perform comprehensive analysis and cache results"""
    global analysis_cache, last_analysis_time
    
    try:
        logger.info("Performing scheduled analysis...")
        
        # Collect data
        dataset = data_collector.get_complete_dataset(days=30)
        
        # Generate AI analysis
        report = ai_analyzer.generate_comprehensive_report(dataset)
        
        # Cache results
        analysis_cache = report
        last_analysis_time = datetime.now()
        
        logger.info("Analysis completed successfully")
        
    except Exception as e:
        logger.error(f"Error in scheduled analysis: {e}")

@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "message": "Workstation AI Decision System",
        "version": "1.0.0",
        "status": "running"
    }

@app.get("/api/analysis/current")
async def get_current_analysis():
    """Get current cached analysis results"""
    global analysis_cache, last_analysis_time
    
    if not analysis_cache:
        # Perform initial analysis if cache is empty
        await perform_analysis_now()
    
    return {
        "analysis": analysis_cache,
        "last_updated": last_analysis_time.isoformat() if last_analysis_time else None,
        "cache_age_minutes": int((datetime.now() - last_analysis_time).total_seconds() / 60) if last_analysis_time else None
    }

@app.post("/api/analysis/refresh")
async def perform_analysis_now():
    """Force refresh of analysis"""
    global analysis_cache, last_analysis_time
    
    try:
        logger.info("Performing manual analysis refresh...")
        
        # Collect fresh data
        dataset = data_collector.get_complete_dataset(days=30)
        
        # Generate AI analysis
        report = ai_analyzer.generate_comprehensive_report(dataset)
        
        # Update cache
        analysis_cache = report
        last_analysis_time = datetime.now()
        
        return {
            "message": "Analysis completed successfully",
            "analysis": report,
            "timestamp": last_analysis_time.isoformat()
        }
        
    except Exception as e:
        logger.error(f"Error in manual analysis: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/analysis/telework")
async def get_telework_analysis():
    """Get telework-specific analysis"""
    try:
        dataset = data_collector.get_complete_dataset(days=30)
        analysis = ai_analyzer.analyze_telework_trends(dataset['telework'], dataset['users'])
        
        return {
            "telework_analysis": analysis,
            "timestamp": datetime.now().isoformat()
        }
        
    except Exception as e:
        logger.error(f"Error in telework analysis: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/analysis/reservation")
async def get_reservation_analysis():
    """Get reservation-specific analysis"""
    try:
        dataset = data_collector.get_complete_dataset(days=30)
        analysis = ai_analyzer.analyze_reservation_occupancy(dataset['reservation'], dataset['workstations'])
        
        return {
            "reservation_analysis": analysis,
            "timestamp": datetime.now().isoformat()
        }
        
    except Exception as e:
        logger.error(f"Error in reservation analysis: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/predictions/occupancy")
async def get_occupancy_predictions(days: int = 30):
    """Get occupancy predictions"""
    try:
        dataset = data_collector.get_complete_dataset(days=60)  # More data for better predictions
        predictions = ai_analyzer.predict_future_trends(dataset['reservation'], days)
        
        return {
            "predictions": predictions,
            "timestamp": datetime.now().isoformat()
        }
        
    except Exception as e:
        logger.error(f"Error in occupancy predictions: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/predictions/telework")
async def get_telework_predictions(days: int = 30):
    """Get telework predictions"""
    try:
        dataset = data_collector.get_complete_dataset(days=60)
        predictions = ai_analyzer.predict_future_trends(dataset['telework'], days)
        
        return {
            "predictions": predictions,
            "timestamp": datetime.now().isoformat()
        }
        
    except Exception as e:
        logger.error(f"Error in telework predictions: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/anomalies")
async def get_anomalies():
    """Get detected anomalies"""
    try:
        dataset = data_collector.get_complete_dataset(days=30)
        
        occupancy_anomalies = ai_analyzer.detect_anomalies(dataset['reservation'])
        telework_anomalies = ai_analyzer.detect_anomalies(dataset['telework'], 'telework_percentage')
        
        return {
            "occupancy_anomalies": occupancy_anomalies,
            "telework_anomalies": telework_anomalies,
            "timestamp": datetime.now().isoformat()
        }
        
    except Exception as e:
        logger.error(f"Error in anomaly detection: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/recommendations")
async def get_recommendations():
    """Get AI recommendations"""
    try:
        dataset = data_collector.get_complete_dataset(days=30)
        report = ai_analyzer.generate_comprehensive_report(dataset)
        
        recommendations = {
            "telework_recommendations": report['telework_analysis']['recommendations'],
            "reservation_recommendations": report['reservation_analysis']['recommendations'],
            "workstation_recommendations": report['reservation_analysis']['workstation_recommendations'],
            "alerts": report['telework_analysis']['alerts'] + report['reservation_analysis']['alerts'],
            "warnings": report['telework_analysis']['warnings'] + report['reservation_analysis']['warnings']
        }
        
        return {
            "recommendations": recommendations,
            "summary": report['summary'],
            "timestamp": datetime.now().isoformat()
        }
        
    except Exception as e:
        logger.error(f"Error in recommendations: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/realtime")
async def get_realtime_data():
    """Get real-time data analysis"""
    try:
        realtime_data = data_collector.get_realtime_data()
        
        if not realtime_data:
            return {"message": "No real-time data available"}
        
        # Quick analysis of real-time data
        analysis = {
            "today_occupancy": 0,
            "today_telework": 0,
            "status": "normal"
        }
        
        if realtime_data.get('reservation'):
            total_workstations = realtime_data['reservation']['total_workstations']
            used_workstations = realtime_data['reservation']['workstations_used']
            
            if total_workstations > 0:
                analysis['today_occupancy'] = round((used_workstations / total_workstations) * 100, 2)
        
        if realtime_data.get('telework'):
            analysis['today_telework'] = realtime_data['telework']['approved_today']
        
        # Determine status
        if analysis['today_occupancy'] > 90:
            analysis['status'] = 'critical_high'
        elif analysis['today_occupancy'] > 80:
            analysis['status'] = 'warning_high'
        elif analysis['today_occupancy'] < 50:
            analysis['status'] = 'warning_low'
        
        return {
            "realtime_data": realtime_data,
            "analysis": analysis,
            "timestamp": datetime.now().isoformat()
        }
        
    except Exception as e:
        logger.error(f"Error in real-time data: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/health")
async def health_check():
    """Health check endpoint"""
    try:
        # Test database connections for all services
        db_services = ["analytics", "contact", "notification", "planning", "reservation", "teletravail", "user"]
        all_healthy = True
        for service in db_services:
            connection = data_collector.get_db_connection(service)
            if connection:
                connection.close()
            else:
                all_healthy = False
                break

        db_status = "healthy" if all_healthy else "unhealthy"

        return {
            "status": "healthy",
            "database": db_status,
            "cache_age_minutes": int((datetime.now() - last_analysis_time).total_seconds() / 60) if last_analysis_time else None,
            "timestamp": datetime.now().isoformat()
        }


    except Exception as e:
        return {
            "status": "unhealthy",
            "error": str(e),
            "timestamp": datetime.now().isoformat()
        }

if __name__ == "__main__":
    uvicorn.run(
        "dashboard_api:app",
        host="0.0.0.0",
        port=8000,
        reload=True,
        log_level="info"
    ) 