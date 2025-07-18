import dash
from dash import dcc, html, Input, Output, callback
import dash_bootstrap_components as dbc
import plotly.graph_objs as go
import plotly.express as px
import pandas as pd
import requests
from datetime import datetime, timedelta
import json
import numpy as np

# Initialize Dash app
app = dash.Dash(__name__, external_stylesheets=[dbc.themes.BOOTSTRAP])
app.title = "Workstation AI Decision Dashboard"

# API base URL
API_BASE_URL = "http://localhost:8000/api"

def create_header():
    """Create the header section"""
    return dbc.Navbar(
        dbc.Container([
            dbc.NavbarBrand("🤖 Workstation AI Decision System", className="ms-2"),
            dbc.Nav([
                dbc.NavItem(dbc.NavLink("Dashboard", href="#")),
                dbc.NavItem(dbc.NavLink("Analytics", href="#")),
                dbc.NavItem(dbc.NavLink("Recommendations", href="#")),
            ]),
            dbc.Button("🔄 Refresh", id="refresh-btn", color="primary", className="ms-2"),
        ]),
        color="dark",
        dark=True,
    )

def create_metrics_cards():
    """Create metrics cards"""
    return dbc.Row([
        dbc.Col([
            dbc.Card([
                dbc.CardBody([
                    html.H4("📊 Occupation Actuelle", className="card-title"),
                    html.H2(id="current-occupancy", children="--%"),
                    html.P("Taux d'occupation des postes", className="card-text")
                ])
            ], color="info", outline=True)
        ], width=3),
        dbc.Col([
            dbc.Card([
                dbc.CardBody([
                    html.H4("🏠 Télétravail", className="card-title"),
                    html.H2(id="current-telework", children="--%"),
                    html.P("Pourcentage de télétravail", className="card-text")
                ])
            ], color="success", outline=True)
        ], width=3),
        dbc.Col([
            dbc.Card([
                dbc.CardBody([
                    html.H4("⚠️ Alertes", className="card-title"),
                    html.H2(id="total-alerts", children="--"),
                    html.P("Alertes critiques actives", className="card-text")
                ])
            ], color="danger", outline=True)
        ], width=3),
        dbc.Col([
            dbc.Card([
                dbc.CardBody([
                    html.H4("💡 Recommandations", className="card-title"),
                    html.H2(id="total-recommendations", children="--"),
                    html.P("Actions recommandées", className="card-text")
                ])
            ], color="warning", outline=True)
        ], width=3),
    ], className="mb-4")

def create_charts():
    """Create charts section"""
    return dbc.Row([
        dbc.Col([
            dbc.Card([
                dbc.CardHeader("📈 Tendance d'Occupation"),
                dbc.CardBody([
                    dcc.Graph(id="occupancy-trend-chart")
                ])
            ])
        ], width=6),
        dbc.Col([
            dbc.Card([
                dbc.CardHeader("🏠 Tendance Télétravail"),
                dbc.CardBody([
                    dcc.Graph(id="telework-trend-chart")
                ])
            ])
        ], width=6),
    ], className="mb-4")

def create_recommendations_section():
    """Create recommendations section"""
    return dbc.Row([
        dbc.Col([
            dbc.Card([
                dbc.CardHeader("🚨 Alertes Critiques"),
                dbc.CardBody(id="critical-alerts")
            ], color="danger", outline=True)
        ], width=6),
        dbc.Col([
            dbc.Card([
                dbc.CardHeader("💡 Recommandations"),
                dbc.CardBody(id="recommendations-list")
            ], color="info", outline=True)
        ], width=6),
    ], className="mb-4")

def create_predictions_section():
    """Create predictions section"""
    return dbc.Row([
        dbc.Col([
            dbc.Card([
                dbc.CardHeader("🔮 Prédictions d'Occupation (30 jours)"),
                dbc.CardBody([
                    dcc.Graph(id="occupancy-prediction-chart")
                ])
            ])
        ], width=6),
        dbc.Col([
            dbc.Card([
                dbc.CardHeader("🔮 Prédictions Télétravail (30 jours)"),
                dbc.CardBody([
                    dcc.Graph(id="telework-prediction-chart")
                ])
            ])
        ], width=6),
    ], className="mb-4")

# App layout
app.layout = dbc.Container([
    create_header(),
    html.Br(),
    create_metrics_cards(),
    create_charts(),
    create_recommendations_section(),
    create_predictions_section(),
    dcc.Interval(
        id='interval-component',
        interval=5*60*1000,  # Update every 5 minutes
        n_intervals=0
    ),
    html.Div(id="last-updated", className="text-muted text-center mt-3")
], fluid=True)

# Callbacks
@app.callback(
    [Output("current-occupancy", "children"),
     Output("current-telework", "children"),
     Output("total-alerts", "children"),
     Output("total-recommendations", "children"),
     Output("last-updated", "children")],
    [Input("refresh-btn", "n_clicks"),
     Input("interval-component", "n_intervals")]
)
def update_metrics(n_clicks, n_intervals):
    """Update metrics cards"""
    try:
        # Get current analysis
        response = requests.get(f"{API_BASE_URL}/analysis/current")
        if response.status_code == 200:
            data = response.json()
            analysis = data.get('analysis', {})
            
            # Extract metrics
            current_occupancy = analysis.get('reservation_analysis', {}).get('current_occupancy', 0)
            current_telework = analysis.get('telework_analysis', {}).get('current_percentage', 0)
            
            total_alerts = len(analysis.get('telework_analysis', {}).get('alerts', [])) + \
                          len(analysis.get('reservation_analysis', {}).get('alerts', []))
            
            total_recommendations = len(analysis.get('telework_analysis', {}).get('recommendations', [])) + \
                                   len(analysis.get('reservation_analysis', {}).get('recommendations', []))
            
            last_updated = data.get('last_updated', 'Unknown')
            if last_updated != 'Unknown':
                last_updated = datetime.fromisoformat(last_updated.replace('Z', '+00:00')).strftime('%Y-%m-%d %H:%M:%S')
            
            return [
                f"{current_occupancy}%",
                f"{current_telework}%",
                total_alerts,
                total_recommendations,
                f"Dernière mise à jour: {last_updated}"
            ]
    except Exception as e:
        print(f"Error updating metrics: {e}")
    
    return ["--%", "--%", "--", "--", "Erreur de mise à jour"]

@app.callback(
    Output("occupancy-trend-chart", "figure"),
    [Input("refresh-btn", "n_clicks"),
     Input("interval-component", "n_intervals")]
)
def update_occupancy_chart(n_clicks, n_intervals):
    """Update occupancy trend chart"""
    try:
        response = requests.get(f"{API_BASE_URL}/analysis/reservation")
        if response.status_code == 200:
            data = response.json()
            analysis = data.get('reservation_analysis', {})
            
            # Create sample data (replace with real data from your database)
            dates = pd.date_range(start=datetime.now() - timedelta(days=30), end=datetime.now(), freq='D')
            occupancy_data = [analysis.get('current_occupancy', 0) + np.random.normal(0, 5) for _ in range(len(dates))]
            
            fig = go.Figure()
            fig.add_trace(go.Scatter(
                x=dates,
                y=occupancy_data,
                mode='lines+markers',
                name='Occupation',
                line=dict(color='blue', width=2)
            ))
            
            # Add threshold lines
            fig.add_hline(y=90, line_dash="dash", line_color="red", annotation_text="Seuil Critique (90%)")
            fig.add_hline(y=80, line_dash="dash", line_color="orange", annotation_text="Seuil d'Alerte (80%)")
            fig.add_hline(y=70, line_dash="dash", line_color="green", annotation_text="Seuil Optimal (70%)")
            
            fig.update_layout(
                title="Tendance d'Occupation des Postes",
                xaxis_title="Date",
                yaxis_title="Taux d'Occupation (%)",
                height=400
            )
            
            return fig
    except Exception as e:
        print(f"Error updating occupancy chart: {e}")
    
    return go.Figure()

@app.callback(
    Output("telework-trend-chart", "figure"),
    [Input("refresh-btn", "n_clicks"),
     Input("interval-component", "n_intervals")]
)
def update_telework_chart(n_clicks, n_intervals):
    """Update telework trend chart"""
    try:
        response = requests.get(f"{API_BASE_URL}/analysis/telework")
        if response.status_code == 200:
            data = response.json()
            analysis = data.get('telework_analysis', {})
            
            # Create sample data
            dates = pd.date_range(start=datetime.now() - timedelta(days=30), end=datetime.now(), freq='D')
            telework_data = [analysis.get('current_percentage', 0) + np.random.normal(0, 3) for _ in range(len(dates))]
            
            fig = go.Figure()
            fig.add_trace(go.Scatter(
                x=dates,
                y=telework_data,
                mode='lines+markers',
                name='Télétravail',
                line=dict(color='green', width=2)
            ))
            
            # Add threshold lines
            fig.add_hline(y=60, line_dash="dash", line_color="red", annotation_text="Seuil Max (60%)")
            fig.add_hline(y=50, line_dash="dash", line_color="orange", annotation_text="Seuil d'Alerte (50%)")
            fig.add_hline(y=30, line_dash="dash", line_color="blue", annotation_text="Seuil Optimal (30%)")
            
            fig.update_layout(
                title="Tendance du Télétravail",
                xaxis_title="Date",
                yaxis_title="Pourcentage de Télétravail (%)",
                height=400
            )
            
            return fig
    except Exception as e:
        print(f"Error updating telework chart: {e}")
    
    return go.Figure()

@app.callback(
    Output("critical-alerts", "children"),
    [Input("refresh-btn", "n_clicks"),
     Input("interval-component", "n_intervals")]
)
def update_critical_alerts(n_clicks, n_intervals):
    """Update critical alerts"""
    try:
        response = requests.get(f"{API_BASE_URL}/analysis/current")
        if response.status_code == 200:
            data = response.json()
            analysis = data.get('analysis', {})
            
            alerts = []
            alerts.extend(analysis.get('telework_analysis', {}).get('alerts', []))
            alerts.extend(analysis.get('reservation_analysis', {}).get('alerts', []))
            
            if not alerts:
                return html.P("Aucune alerte critique", className="text-success")
            
            alert_items = []
            for alert in alerts:
                alert_items.append(
                    dbc.Alert([
                        html.H6(alert.get('message', 'Alerte'), className="alert-heading"),
                        html.P(alert.get('action', 'Action requise'), className="mb-0")
                    ], color="danger", className="mb-2")
                )
            
            return html.Div(alert_items)
    except Exception as e:
        print(f"Error updating alerts: {e}")
    
    return html.P("Erreur de chargement des alertes", className="text-danger")

@app.callback(
    Output("recommendations-list", "children"),
    [Input("refresh-btn", "n_clicks"),
     Input("interval-component", "n_intervals")]
)
def update_recommendations(n_clicks, n_intervals):
    """Update recommendations"""
    try:
        response = requests.get(f"{API_BASE_URL}/recommendations")
        if response.status_code == 200:
            data = response.json()
            recommendations = data.get('recommendations', {})
            
            all_recommendations = []
            all_recommendations.extend(recommendations.get('telework_recommendations', []))
            all_recommendations.extend(recommendations.get('reservation_recommendations', []))
            
            if not all_recommendations:
                return html.P("Aucune recommandation", className="text-muted")
            
            rec_items = []
            for rec in all_recommendations:
                rec_items.append(
                    dbc.Alert([
                        html.H6(rec.get('message', 'Recommandation'), className="alert-heading"),
                        html.P(rec.get('action', 'Action suggérée'), className="mb-0")
                    ], color="info", className="mb-2")
                )
            
            return html.Div(rec_items)
    except Exception as e:
        print(f"Error updating recommendations: {e}")
    
    return html.P("Erreur de chargement des recommandations", className="text-danger")

@app.callback(
    Output("occupancy-prediction-chart", "figure"),
    [Input("refresh-btn", "n_clicks"),
     Input("interval-component", "n_intervals")]
)
def update_occupancy_predictions(n_clicks, n_intervals):
    """Update occupancy predictions"""
    try:
        response = requests.get(f"{API_BASE_URL}/predictions/occupancy")
        if response.status_code == 200:
            data = response.json()
            predictions = data.get('predictions', {})
            
            if not predictions.get('predictions'):
                return go.Figure()
            
            pred_data = predictions['predictions']
            dates = [pred['date'] for pred in pred_data]
            values = [pred['predicted_value'] for pred in pred_data]
            
            fig = go.Figure()
            fig.add_trace(go.Scatter(
                x=dates,
                y=values,
                mode='lines+markers',
                name='Prédiction',
                line=dict(color='purple', width=2)
            ))
            
            fig.update_layout(
                title="Prédiction d'Occupation (30 jours)",
                xaxis_title="Date",
                yaxis_title="Taux d'Occupation Prédit (%)",
                height=400
            )
            
            return fig
    except Exception as e:
        print(f"Error updating occupancy predictions: {e}")
    
    return go.Figure()

@app.callback(
    Output("telework-prediction-chart", "figure"),
    [Input("refresh-btn", "n_clicks"),
     Input("interval-component", "n_intervals")]
)
def update_telework_predictions(n_clicks, n_intervals):
    """Update telework predictions"""
    try:
        response = requests.get(f"{API_BASE_URL}/predictions/telework")
        if response.status_code == 200:
            data = response.json()
            predictions = data.get('predictions', {})
            
            if not predictions.get('predictions'):
                return go.Figure()
            
            pred_data = predictions['predictions']
            dates = [pred['date'] for pred in pred_data]
            values = [pred['predicted_value'] for pred in pred_data]
            
            fig = go.Figure()
            fig.add_trace(go.Scatter(
                x=dates,
                y=values,
                mode='lines+markers',
                name='Prédiction',
                line=dict(color='orange', width=2)
            ))
            
            fig.update_layout(
                title="Prédiction Télétravail (30 jours)",
                xaxis_title="Date",
                yaxis_title="Pourcentage Télétravail Prédit (%)",
                height=400
            )
            
            return fig
    except Exception as e:
        print(f"Error updating telework predictions: {e}")
    
    return go.Figure()

if __name__ == '__main__':
    app.run_server(debug=True, host='0.0.0.0', port=8050) 