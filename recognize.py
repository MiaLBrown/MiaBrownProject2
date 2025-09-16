from flask import Flask, request, jsonify
from flask_cors import CORS
import face_recognition
import numpy as np
from sklearn.neighbors import KNeighborsClassifier
import pickle

app = Flask(__name__)
CORS(app)  # Allow connections from your app

# Load the pre-trained KNN model and labels
with open("knn_model.pkl", "rb") as model_file:
    model = pickle.load(model_file)
with open("labels.pkl", "rb") as labels_file:
    label_names = pickle.load(labels_file)

@app.route("/recognize", methods=["POST"])
def recognize():
    print("Request received")
    
    # Check if the 'image' field is present in the request
    if 'image' not in request.files:
        print("No image file provided")
        return jsonify({"error": "No image file provided"}), 400
    
    file = request.files['image']
    
    try:
        # Load and process the image
        img = face_recognition.load_image_file(file)
        face_locations = face_recognition.face_locations(img)
        
        if len(face_locations) != 1:
            print("Image must contain exactly one face")
            return jsonify({"error": "Image must contain exactly one face"}), 400
        
        # Extract the face encoding
        face_encoding = face_recognition.face_encodings(img, known_face_locations=face_locations)[0]
        face_encoding = np.array([face_encoding])  # Model expects 2D array
        
        # Directly get the name from the model's prediction
        predicted_name = model.predict(face_encoding)[0]  # Assuming it returns a string
        predicted_proba = model.predict_proba(face_encoding)[0]  # Get the probability array
        confidence = predicted_proba.max()  # Get the highest confidence value
        
        # Threshold for valid recognition
        threshold = 0.6
        if confidence < threshold:
            print("Face not confidently matched")
            return jsonify({"status": "failure", "name": "unknown"}), 200
        
        # Return the predicted name and confidence if successful
        return jsonify({"name": predicted_name}), 200

    except Exception as e:
        print(f"Error processing image: {e}")
        return jsonify({"error": "Failed to recognize face"}), 500

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
