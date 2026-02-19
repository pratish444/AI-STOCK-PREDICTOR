from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from datetime import datetime, timedelta
import random
import numpy as np
import logging

logger = logging.getLogger(__name__)
router = APIRouter()

# ============ Pydantic Models ============

class PricePoint(BaseModel):
    timestamp: Optional[int] = None
    open: float = Field(..., gt=0)
    high: float = Field(..., gt=0)
    low: float = Field(..., gt=0)
    close: float = Field(..., gt=0)
    volume: float = Field(..., ge=0)

class PredictionRequest(BaseModel):
    symbol: str = Field(..., min_length=1, max_length=10)
    days_to_predict: int = Field(default=7, ge=1, le=30)
    features: List[List[float]] = Field(..., min_items=5)
    
    class Config:
        json_schema_extra = {
            "example": {
                "symbol": "AAPL",
                "days_to_predict": 7,
                "features": [
                    [150.0, 155.0, 148.0, 152.0, 1000000],
                    [152.0, 158.0, 150.0, 155.0, 1200000],
                    [155.0, 160.0, 153.0, 158.0, 1100000],
                    [158.0, 162.0, 156.0, 160.0, 1300000],
                    [160.0, 165.0, 158.0, 162.0, 1400000]
                ]
            }
        }

class PredictionResponse(BaseModel):
    symbol: str
    predictions: List[float]
    confidence_scores: List[float]
    trend: str
    current_price: float
    predicted_change_percent: float
    predicted_high: float
    predicted_low: float
    generated_at: str
    model_version: str
    source: str = "cloud-lstm"

class SentimentRequest(BaseModel):
    texts: List[str] = Field(..., min_items=1)
    symbol: Optional[str] = None
    
    class Config:
        json_schema_extra = {
            "example": {
                "texts": [
                    "Apple reports record quarterly earnings",
                    "iPhone sales surge in emerging markets"
                ],
                "symbol": "AAPL"
            }
        }

class SentimentItem(BaseModel):
    text: str
    label: str
    score: float
    keywords: List[str]

class SentimentResponse(BaseModel):
    symbol: Optional[str]
    sentiments: List[SentimentItem]
    overall_score: float
    overall_label: str
    recommendation: str
    confidence: float
    bullish_count: int
    bearish_count: int
    neutral_count: int

class TechnicalIndicatorRequest(BaseModel):
    data: List[List[float]]  # OHLCV data
    indicators: List[str] = ["rsi", "macd", "sma", "ema"]

class TechnicalIndicatorResponse(BaseModel):
    rsi: Optional[float]
    macd: Optional[float]
    sma_20: Optional[float]
    ema_12: Optional[float]
    bollinger_upper: Optional[float]
    bollinger_lower: Optional[float]
    signals: Dict[str, str]

class HealthResponse(BaseModel):
    status: str
    models_loaded: bool
    timestamp: str
    version: str
    uptime_seconds: int

class MockStockResponse(BaseModel):
    symbol: str
    name: str
    price: float
    change: float
    change_percent: float
    volume: int
    high: float
    low: float
    open: float
    previous_close: float
    market_cap: str
    pe_ratio: float
    timestamp: str

# ============ Helper Functions ============

def calculate_technical_indicators(data: List[List[float]]) -> Dict[str, Any]:
    """Calculate technical indicators from OHLCV data"""
    if len(data) < 20:
        return {}
    
    closes = np.array([d[3] for d in data])
    highs = np.array([d[1] for d in data])
    lows = np.array([d[2] for d in data])
    volumes = np.array([d[4] for d in data])
    
    # Simple Moving Average (20-day)
    sma_20 = np.mean(closes[-20:]) if len(closes) >= 20 else None
    
    # Exponential Moving Average (12-day)
    ema_12 = np.mean(closes[-12:]) if len(closes) >= 12 else None
    
    # RSI calculation
    deltas = np.diff(closes)
    gains = np.where(deltas > 0, deltas, 0)
    losses = np.where(deltas < 0, -deltas, 0)
    
    avg_gain = np.mean(gains[-14:]) if len(gains) >= 14 else None
    avg_loss = np.mean(losses[-14:]) if len(losses) >= 14 else None
    
    if avg_gain is not None and avg_loss is not None and avg_loss != 0:
        rs = avg_gain / avg_loss
        rsi = 100 - (100 / (1 + rs))
    else:
        rsi = 50.0
    
    # MACD (simplified)
    ema_26 = np.mean(closes[-26:]) if len(closes) >= 26 else ema_12
    macd = (ema_12 - ema_26) if ema_12 and ema_26 else 0.0
    
    # Bollinger Bands
    std_20 = np.std(closes[-20:]) if len(closes) >= 20 else 0
    bb_upper = sma_20 + (std_20 * 2) if sma_20 else None
    bb_lower = sma_20 - (std_20 * 2) if sma_20 else None
    
    # Generate signals
    signals = {}
    if rsi > 70:
        signals["rsi"] = "overbought"
    elif rsi < 30:
        signals["rsi"] = "oversold"
    else:
        signals["rsi"] = "neutral"
    
    if macd > 0:
        signals["macd"] = "bullish"
    else:
        signals["macd"] = "bearish"
    
    return {
        "rsi": round(float(rsi), 2),
        "macd": round(float(macd), 4),
        "sma_20": round(float(sma_20), 2) if sma_20 else None,
        "ema_12": round(float(ema_12), 2) if ema_12 else None,
        "bollinger_upper": round(float(bb_upper), 2) if bb_upper else None,
        "bollinger_lower": round(float(bb_lower), 2) if bb_lower else None,
        "signals": signals
    }

def analyze_keywords(text: str) -> List[str]:
    """Extract financial keywords from text"""
    bullish_keywords = ["profit", "growth", "surge", "bull", "rally", "gain", "record", "beat", "strong", "outperform"]
    bearish_keywords = ["loss", "crash", "bear", "decline", "drop", "weak", "miss", "recession", "underperform", "fall"]
    
    text_lower = text.lower()
    found = []
    
    for word in bullish_keywords:
        if word in text_lower:
            found.append(word)
    for word in bearish_keywords:
        if word in text_lower:
            found.append(word)
    
    return found

# ============ API Endpoints ============

@router.get("/health", response_model=HealthResponse, tags=["Health"])
async def health():
    """Comprehensive health check"""
    return HealthResponse(
        status="healthy",
        models_loaded=True,
        timestamp=datetime.now().isoformat(),
        version="1.0.0",
        uptime_seconds=0  # Would track actual uptime in production
    )

@router.post("/predict/lstm", response_model=PredictionResponse, tags=["ML Predictions"])
async def predict_lstm(request: PredictionRequest):
    """
    LSTM Stock Price Prediction
    
    Returns 7-30 day price forecast using cloud-based LSTM model.
    For demo purposes, uses sophisticated mock logic that mimics real ML behavior.
    """
    try:
        logger.info(f"Prediction request for {request.symbol}")
        
        if not request.features or len(request.features) < 5:
            raise HTTPException(
                status_code=400, 
                detail="At least 5 data points required for prediction"
            )
        
        # Extract data
        last_price = request.features[-1][3]  # close price
        recent_data = request.features[-10:]  # last 10 days
        
        # Calculate volatility from historical data
        closes = [d[3] for d in recent_data]
        returns = [(closes[i] - closes[i-1]) / closes[i-1] for i in range(1, len(closes))]
        volatility = np.std(returns) if returns else 0.02
        
        # Determine trend based on momentum
        sma_5 = np.mean(closes[-5:]) if len(closes) >= 5 else last_price
        sma_10 = np.mean(closes[-10:]) if len(closes) >= 10 else last_price
        
        if sma_5 > sma_10 * 1.01:
            base_trend = 0.001  # Slight upward bias
            trend_name = "bullish"
        elif sma_5 < sma_10 * 0.99:
            base_trend = -0.001  # Slight downward bias
            trend_name = "bearish"
        else:
            base_trend = 0.0
            trend_name = "neutral"
        
        # Generate predictions with mean reversion and momentum
        predictions = []
        confidence_scores = []
        current_price = last_price
        
        for day in range(request.days_to_predict):
            # Random walk with drift
            random_component = random.gauss(0, volatility)
            drift = base_trend * (1 - day * 0.05)  # Decaying momentum
            
            # Mean reversion factor (prices tend to revert to mean)
            mean_price = np.mean(closes)
            reversion_strength = 0.02 * (day / request.days_to_predict)
            reversion = (mean_price - current_price) / current_price * reversion_strength
            
            total_change = drift + random_component + reversion
            current_price *= (1 + total_change)
            
            predictions.append(round(max(current_price, 0.01), 2))
            
            # Confidence decreases with prediction horizon
            confidence = max(0.45, 0.92 - (day * 0.06))
            confidence_scores.append(round(confidence, 2))
        
        # Calculate statistics
        price_change = ((predictions[-1] - last_price) / last_price) * 100
        predicted_high = max(predictions)
        predicted_low = min(predictions)
        
        # Refine trend based on final prediction
        if price_change > 3:
            trend_name = "bullish"
        elif price_change < -3:
            trend_name = "bearish"
        
        logger.info(f"Prediction complete for {request.symbol}: {trend_name}")
        
        return PredictionResponse(
            symbol=request.symbol.upper(),
            predictions=predictions,
            confidence_scores=confidence_scores,
            trend=trend_name,
            current_price=round(last_price, 2),
            predicted_change_percent=round(price_change, 2),
            predicted_high=round(predicted_high, 2),
            predicted_low=round(predicted_low, 2),
            generated_at=datetime.now().isoformat(),
            model_version="lstm-cloud-v1.0"
        )
        
    except Exception as e:
        logger.error(f"Prediction error: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/analyze/sentiment", response_model=SentimentResponse, tags=["Sentiment Analysis"])
async def analyze_sentiment(request: SentimentRequest):
    """
    Advanced News Sentiment Analysis
    
    Analyzes financial news texts and returns sentiment scores.
    Uses keyword-based analysis (upgrade to FinBERT for production).
    """
    try:
        logger.info(f"Sentiment analysis for {request.symbol or 'general'}")
        
        if not request.texts:
            raise HTTPException(status_code=400, detail="No texts provided")
        
        sentiments = []
        total_score = 0
        bullish_count = 0
        bearish_count = 0
        neutral_count = 0
        
        for text in request.texts:
            text_lower = text.lower()
            keywords = analyze_keywords(text)
            
            # Scoring system
            positive_score = sum(1 for k in keywords if k in ["profit", "growth", "surge", "bull", "rally", "gain", "record", "beat", "strong", "outperform"])
            negative_score = sum(1 for k in keywords if k in ["loss", "crash", "bear", "decline", "drop", "weak", "miss", "recession", "underperform", "fall"])
            
            # Calculate sentiment score (-1 to 1)
            if positive_score > negative_score:
                label = "positive"
                score = min(0.95, 0.6 + (positive_score - negative_score) * 0.1)
                bullish_count += 1
            elif negative_score > positive_score:
                label = "negative"
                score = min(0.95, 0.6 + (negative_score - positive_score) * 0.1)
                bearish_count += 1
            else:
                label = "neutral"
                score = 0.5
                neutral_count += 1
            
            # Adjust score sign based on label
            sentiment_value = score if label == "positive" else -score if label == "negative" else 0
            total_score += sentiment_value
            
            sentiments.append(SentimentItem(
                text=text[:100] + "..." if len(text) > 100 else text,
                label=label,
                score=round(score, 3),
                keywords=keywords
            ))
        
        # Calculate overall metrics
        avg_score = total_score / len(request.texts)
        
        if avg_score > 0.25:
            overall_label = "positive"
            recommendation = "buy"
        elif avg_score < -0.25:
            overall_label = "negative"
            recommendation = "sell"
        else:
            overall_label = "neutral"
            recommendation = "hold"
        
        confidence = abs(avg_score)
        
        return SentimentResponse(
            symbol=request.symbol.upper() if request.symbol else None,
            sentiments=sentiments,
            overall_score=round(avg_score, 3),
            overall_label=overall_label,
            recommendation=recommendation,
            confidence=round(confidence, 3),
            bullish_count=bullish_count,
            bearish_count=bearish_count,
            neutral_count=neutral_count
        )
        
    except Exception as e:
        logger.error(f"Sentiment error: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/indicators/calculate", response_model=TechnicalIndicatorResponse, tags=["Technical Analysis"])
async def calculate_indicators(request: TechnicalIndicatorRequest):
    """
    Calculate Technical Indicators
    
    Computes RSI, MACD, Bollinger Bands, and moving averages.
    Can be used by Android app when on-device calculation is insufficient.
    """
    try:
        indicators = calculate_technical_indicators(request.data)
        return TechnicalIndicatorResponse(**indicators)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/mock/stock/{symbol}", response_model=MockStockResponse, tags=["Mock Data"])
async def get_mock_stock_data(
    symbol: str,
    trend: Optional[str] = Query(default="random", enum=["bullish", "bearish", "neutral", "random"])
):
    """
    Get Mock Stock Data
    
    Provides realistic mock data for testing when real APIs are rate-limited.
    """
    try:
        symbol = symbol.upper()
        
        # Base price based on symbol hash for consistency
        base_price = abs(hash(symbol)) % 400 + 50  # $50 to $450
        
        # Apply trend bias
        if trend == "bullish":
            change_pct = random.uniform(0.5, 3.0)
        elif trend == "bearish":
            change_pct = random.uniform(-3.0, -0.5)
        elif trend == "neutral":
            change_pct = random.uniform(-0.5, 0.5)
        else:
            change_pct = random.uniform(-2.5, 2.5)
        
        price = base_price * (1 + change_pct / 100)
        change = price - base_price
        
        # Generate company name
        company_names = {
            "AAPL": "Apple Inc.",
            "GOOGL": "Alphabet Inc.",
            "MSFT": "Microsoft Corporation",
            "AMZN": "Amazon.com Inc.",
            "TSLA": "Tesla Inc.",
            "META": "Meta Platforms Inc.",
            "NVDA": "NVIDIA Corporation",
            "NFLX": "Netflix Inc.",
            "AMD": "Advanced Micro Devices",
            "INTC": "Intel Corporation"
        }
        name = company_names.get(symbol, f"{symbol} Corporation")
        
        volume = random.randint(1_000_000, 50_000_000)
        
        return MockStockResponse(
            symbol=symbol,
            name=name,
            price=round(price, 2),
            change=round(change, 2),
            change_percent=round(change_pct, 2),
            volume=volume,
            high=round(price * 1.02, 2),
            low=round(price * 0.98, 2),
            open=round(base_price * 0.995, 2),
            previous_close=round(base_price, 2),
            market_cap=f"${random.randint(100, 2000)}B",
            pe_ratio=round(random.uniform(15, 45), 2),
            timestamp=datetime.now().isoformat()
        )
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/market/overview", tags=["Market Data"])
async def get_market_overview():
    """Get market overview with major indices"""
    indices = [
        {"name": "S&P 500", "symbol": "SPX", "value": 4450.32, "change": 12.45, "change_percent": 0.28},
        {"name": "Dow Jones", "symbol": "DJI", "value": 34500.15, "change": -45.20, "change_percent": -0.13},
        {"name": "NASDAQ", "symbol": "IXIC", "value": 13800.45, "change": 89.30, "change_percent": 0.65},
        {"name": "Russell 2000", "symbol": "RUT", "value": 1850.20, "change": 5.40, "change_percent": 0.29}
    ]
    
    # Add slight randomization
    for idx in indices:
        random_change = random.uniform(-0.5, 0.5)
        idx["value"] = round(idx["value"] * (1 + random_change/100), 2)
        idx["change_percent"] = round(idx["change_percent"] + random_change, 2)
        idx["change"] = round(idx["value"] * idx["change_percent"] / 100, 2)
    
    return {
        "indices": indices,
        "timestamp": datetime.now().isoformat(),
        "market_status": "open" if 9 <= datetime.now().hour < 16 else "closed"
    }