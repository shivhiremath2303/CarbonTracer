import io
import pandas as pd
import numpy as np
import calendar
from sklearn.preprocessing import LabelEncoder, MinMaxScaler
from sklearn.model_selection import train_test_split
from xgboost import XGBRegressor
from tensorflow.keras.models import Model
from tensorflow.keras.layers import Input, LSTM, Dense
from tensorflow.keras.optimizers import Adam
from google.colab import files

# Upload prediction dataset
print("Please upload the dataset for prediction model:")
uploaded_pred = files.upload()
file_name_pred = next(iter(uploaded_pred))
df_pred = pd.read_excel(io.BytesIO(uploaded_pred[file_name_pred]))

# Upload suggestion dataset
print("\nPlease upload the dataset for suggestion and clustering:")
uploaded_sugg = files.upload()
file_name_sugg = next(iter(uploaded_sugg))
df_sugg = pd.read_excel(io.BytesIO(uploaded_sugg[file_name_sugg]))

# Prediction model training
df_pred['Car_Fuel_Type'] = df_pred['Car_Fuel_Type'].fillna('None')
le = LabelEncoder()
df_pred['Car_Fuel_Type_enc'] = le.fit_transform(df_pred['Car_Fuel_Type'])

drop_cols = ['Household_ID', 'Car_Fuel_Type', 'MonthStart', 'Total_CO2_kg']
exclude_cols = [
    'Emis_Electricity_kgCO2', 'Emis_LPG_kgCO2', 'Emis_PNG_kgCO2', 'Emis_Wood_kgCO2',
    'Emis_Kerosene_kgCO2', 'Emis_Charcoal_kgCO2', 'Emis_Coal_kgCO2', 'Emis_Propane_kgCO2',
    'Emis_Petrol_kgCO2', 'Emis_Diesel_kgCO2', 'Emis_Bike_kgCO2'
]
features = df_pred.drop(columns=drop_cols + exclude_cols, errors='ignore')
target = df_pred['Total_CO2_kg']

X_train, X_test, y_train, y_test = train_test_split(features, target, test_size=0.2, random_state=42)
model = XGBRegressor(n_estimators=100, max_depth=4, learning_rate=0.1, subsample=0.6, random_state=42)
model.fit(X_train, y_train)

monthly_emissions = df_pred.groupby(['Year', 'Month'])['Total_CO2_kg'].mean().reset_index()
monthly_emissions['Date'] = pd.to_datetime(monthly_emissions[['Year', 'Month']].assign(DAY=1))
monthly_emissions.sort_values('Date', inplace=True)
emission_series = monthly_emissions['Total_CO2_kg'].values.reshape(-1, 1)

emission_scaler = MinMaxScaler()
emission_scaled = emission_scaler.fit_transform(emission_series)

def create_sequences(data, n_steps=3):
    X, y = [], []
    for i in range(len(data) - n_steps):
        X.append(data[i:i+n_steps])
        y.append(data[i+n_steps])
    return np.array(X), np.array(y)

n_steps = 3
X_seq, y_seq = create_sequences(emission_scaled, n_steps)
input_layer = Input(shape=(n_steps, 1))
lstm_layer = LSTM(50, activation='relu')(input_layer)
output_layer = Dense(1)(lstm_layer)
lstm_model = Model(inputs=input_layer, outputs=output_layer)
lstm_model.compile(optimizer=Adam(learning_rate=0.001), loss='mse')
lstm_model.fit(X_seq, y_seq, epochs=50, verbose=1)

def get_float(prompt):
    val = input(prompt)
    return float(val) if val.strip() else 0.0

print("\nEnter current month\'s consumption details:")
user_var_input = {
    'Year': int(input("Year (e.g., 2025): ")),
    'Month': int(input("Month (1-12): ")),
    'Electricity_kWh_total': get_float("Electricity (kWh total): "),
    'LPG_kg': get_float("LPG (kg): "),
    'Petrol_L': get_float("Petrol (liters): "),
    'Diesel_L': get_float("Diesel (liters): "),
    'PNG_scm': get_float("PNG (scm): "),
}

fixed_features = [
    'Household_Size', 'Urban', 'Has_Car', 'Car_CC', 'Car_Fuel_Type_enc',
    'Has_Bike', 'Bike_CC', 'Bike_Monthly_km', 'Bike_Fuel_L',
    'Car_Monthly_km', 'Car_Fuel_L', 'AC_Tonnage', 'AC_Hours_per_day',
    'Electricity_kWh_other', 'Electricity_kWh_AC'
]
fixed_vals = {f: df_pred[f].mode()[0] if f in df_pred.columns else 0 for f in fixed_features}
complete_input = {**user_var_input, **fixed_vals}
complete_input['Car_Fuel_Type_enc'] = fixed_vals['Car_Fuel_Type_enc']

input_df = pd.DataFrame([complete_input])
input_df = input_df.reindex(columns=X_train.columns, fill_value=0)

pred_current = model.predict(input_df)[0]
print(f"\nPredicted Carbon Emission for {calendar.month_name[user_var_input['Month']]} {user_var_input['Year']}: {pred_current:.2f} kg CO2")

pred_scaled = emission_scaler.transform(np.array([[pred_current]]))[0, 0]
input_seq = emission_scaled[-n_steps:].copy()
input_seq[-1, 0] = pred_scaled

future_preds_scaled = []
for _ in range(6):
    pred = lstm_model.predict(input_seq.reshape(1, n_steps, 1), verbose=0)[0, 0]
    future_preds_scaled.append(pred)
    input_seq = np.append(input_seq[1:], pred).reshape(-1, 1)

future_preds = emission_scaler.inverse_transform(np.array(future_preds_scaled).reshape(-1, 1)).flatten()

print("\nPredicted Carbon Emissions for next 6 months:")
for i, val in enumerate(future_preds, 1):
    month = (user_var_input['Month'] + i - 1) % 12 + 1
    year = user_var_input['Year'] + ((user_var_input['Month'] + i - 1) // 12)
    print(f"{calendar.month_name[month]} {year}: {val:.2f} kg CO2")

# Suggestion set processing
df_sugg.sort_values(['Household_ID', 'Year', 'Month'], inplace=True)
df_sugg['Prev_Month_Emission'] = df_sugg.groupby('Household_ID')['Total_Carbon_Emission'].shift(1)
df_sugg['Emission_Diff'] = df_sugg['Total_Carbon_Emission'] - df_sugg['Prev_Month_Emission']
df_sugg['Emission_Increase'] = df_sugg['Emission_Diff'] > 0

def generate_always_on_suggestions(row):
    suggestions = []
    suggestions.append("Consider adopting energy-saving habits and optimizing appliance usage to reduce emissions.")

    if row.get('Electricity_Consumption_kWh', 0) > 600:
        suggestions.append("High electricity use detected; consider efficient appliances and reduced usage.")
        if row.get('AC_Hours_per_day', 0) > 2:
            suggestions.append("Reduce AC hours or switch to more efficient AC units.")
        if row.get('Fridge_Count', 0) > 1:
            suggestions.append("Use energy-efficient refrigerators and limit number.")

    if row.get('LPG_Consumption_kg', 0) > 10:
        suggestions.append("Optimize LPG usage by efficient cooking or reducing cylinders.")

    if row.get('Petrol_Consumption_L', 0) > 20:
        suggestions.append("Reduce petrol use; consider efficient vehicles or carpooling.")

    if row.get('Diesel_Consumption_L', 0) > 20:
        suggestions.append("Reduce diesel use; consider alternatives or low emission vehicles.")

    if row.get('CNG_Consumption_kg', 0) > 20:
        suggestions.append("Optimize CNG vehicle usage.")

    if row.get('Firewood_kg', 0) > 10:
        suggestions.append("Use efficient cookstoves to reduce firewood.")

    if row.get('Kerosene_Liters', 0) > 10:
        suggestions.append("Switch to efficient lighting/heating instead of kerosene.")

    if row.get('Backup_Generator_Diesel_Liters', 0) > 10:
        suggestions.append("Minimize generator use; consider clean alternatives.")

    if 'Refrigerator_Star_Rating' in row and row['Refrigerator_Star_Rating'] < 3:
        suggestions.append("Upgrade to higher star-rated refrigerator for energy savings.")

    return suggestions

def get_closest_or_group_suggestions(df_sugg, user_var_input):
    household_rows = df_sugg[df_sugg['Household_ID'] == df_sugg['Household_ID'].iloc[0]]  # Update with actual household_id if possible

    household_rows = household_rows.copy()
    household_rows['MonthDiff'] = abs(
        (household_rows['Year'] - user_var_input['Year']) * 12 + household_rows['Month'] - user_var_input['Month']
    )

    closest_row = household_rows.loc[household_rows['MonthDiff'].idxmin()]

    suggestions = generate_always_on_suggestions(closest_row)

    return suggestions

household_id = df_sugg['Household_ID'].iloc[0]
user_row = df_sugg[(df_sugg['Household_ID'] == household_id) &
                   (df_sugg['Year'] == user_var_input['Year']) &
                   (df_sugg['Month'] == user_var_input['Month'])]

if len(user_row) == 0:
    print("\nProviding suggestions based on closest available data.")
    suggestions = get_closest_or_group_suggestions(df_sugg, user_var_input)
else:
    user_row = user_row.iloc[0]
    suggestions = generate_always_on_suggestions(user_row)

print("\nSuggestions based on your consumption:")
for s in suggestions:
    print("- " + s)
