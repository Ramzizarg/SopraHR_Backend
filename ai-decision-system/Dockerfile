# Multi-service: FastAPI API on 8000, Dash Dashboard on 8050
FROM python:3.9-slim

WORKDIR /app

# Install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY . .

# Expose ports
EXPOSE 8000 8005

# Run both services (use supervisor or separate commands; here, assume main.py starts API, dashboard.py starts Dash)
# For simplicity, start FastAPI (adjust if dashboard is separate process)
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
# To run dashboard concurrently, use: CMD ["python", "ai_dashboard.py"] or add supervisor
