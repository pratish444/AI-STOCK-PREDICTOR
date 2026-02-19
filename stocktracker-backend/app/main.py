from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from app.api import router
import logging

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Create FastAPI app
app = FastAPI(
    title="StockTracker ML API",
    description="""
    Hybrid ML Backend for StockTracker Android Application.
    
    ## Features
    * **LSTM Predictions**: Cloud-based complex price forecasting
    * **Sentiment Analysis**: News sentiment using ML models
    * **Mock Data**: Fallback when APIs are rate-limited
    * **Health Monitoring**: System status and diagnostics
    
    ## Hybrid Approach
    - On-device (Android): Simple indicators, offline capability
    - Cloud (This API): Complex LSTM models, advanced sentiment analysis
    """,
    version="1.0.0",
    contact={
        "name": "StockTracker Team",
        "email": "support@stocktracker.com"
    }
)

# CORS Configuration - CRITICAL for Android connection
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # In production, restrict to your app domain
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include all API routes
app.include_router(router, prefix="/api/v1")

@app.get("/", tags=["Root"])
async def root():
    """Root endpoint with API information"""
    return {
        "message": "StockTracker ML API is running",
        "version": "1.0.0",
        "documentation": "/docs",
        "health_check": "/api/v1/health",
        "endpoints": {
            "prediction": "/api/v1/predict/lstm",
            "sentiment": "/api/v1/analyze/sentiment",
            "mock_data": "/api/v1/mock/stock/{symbol}",
            "technical_indicators": "/api/v1/indicators/calculate"
        },
        "status": "operational"
    }

@app.get("/health", tags=["Health"])
async def health_check():
    """Simple health check for load balancers"""
    return {"status": "healthy"}

@app.exception_handler(Exception)
async def global_exception_handler(request, exc):
    """Global exception handler"""
    logger.error(f"Global error: {str(exc)}")
    return JSONResponse(
        status_code=500,
        content={"detail": "Internal server error", "error": str(exc)}
    )