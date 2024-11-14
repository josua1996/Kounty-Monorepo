const express = require('express');
const mongoose = require('mongoose');
const dotenv = require('dotenv');
const admin = require('firebase-admin');

// Cargar variables de entorno
dotenv.config();

// Inicializar Firebase Admin SDK
const serviceAccount = require('./config/firebase-credentials.json');
admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
});

// Crear una instancia de Express
const app = express();
const PORT = process.env.PORT || 3000;

// Middleware para parsear el cuerpo de las solicitudes
app.use(express.json());

// Conectar a MongoDB
const connectDB = async () => {
    try {
        await mongoose.connect(process.env.MONGODB_URI);
        console.log('Conectado a la base de datos MongoDB');
    } catch (error) {
        console.error(`Error al conectar a MongoDB: ${error.message}`);
        process.exit(1);
    }
};
connectDB();

// Modelos de Mongoose
const usuarioSchema = new mongoose.Schema({
    usuarioId: { type: String, required: true, unique: true },
    token: { type: String, required: true },
});
const Usuario = mongoose.model('Usuario', usuarioSchema);

const recordatorioSchema = new mongoose.Schema({
    usuarioId: { type: String, required: true },
    fecha: { type: Date, required: true },
    tipoImpuesto: { type: String, required: true },
    mensaje: { type: String, required: true },
});
const Recordatorio = mongoose.model('Recordatorio', recordatorioSchema);

// Middleware para verificar el token de Firebase
const verificarToken = async (req, res, next) => {
    const token = req.headers['authorization'];
    if (!token) {
        console.log('Token no proporcionado');
        return res.status(403).send('Token no proporcionado');
    }

    try {
        const tokenValue = token.replace("Bearer ", "");
        const decodedToken = await admin.auth().verifyIdToken(tokenValue);
        req.user = decodedToken;
        next();
    } catch (error) {
        console.error('Error al verificar el token:', error);
        return res.status(401).send('Token inválido o expirado');
    }
};

// Rutas

app.post('/register-usuario', async (req, res) => {
    try {
        const { usuarioId, token } = req.body;
        const usuarioExistente = await Usuario.findOne({ usuarioId });
        
        if (usuarioExistente) {
            return res.status(400).json({ mensaje: 'Usuario ya registrado' });
        }

        const nuevoUsuario = new Usuario({ usuarioId, token });
        await nuevoUsuario.save();

        res.status(201).json({ mensaje: 'Usuario registrado exitosamente' });
    } catch (error) {
        res.status(500).json({ mensaje: 'Error al registrar usuario', error });
    }
});

app.get('/', (req, res) => {
    res.send('¡El asistente está funcionando!');
});

// Actualizar el token del usuario
app.post('/actualizar-token', verificarToken, async (req, res) => {
    try {
        const { usuarioId, newToken } = req.body;  // Recibimos ambos valores
        if (!newToken) {
            return res.status(400).json({ error: "Token inválido o no proporcionado" });
        }

        const usuario = await Usuario.findOne({ usuarioId });

        if (!usuario) {
            const nuevoUsuario = new Usuario({ usuarioId, token: newToken });
            await nuevoUsuario.save();
            res.status(200).json({ message: "Token actualizado correctamente" });
        } else {
            usuario.token = newToken;
            await usuario.save();
            res.status(200).json({ message: "Token actualizado correctamente" });
        }
    } catch (error) {
        console.error("Error al actualizar el token:", error);
        res.status(500).json({ error: "Error interno del servidor" });
    }
});

// Agregar un recordatorio
app.post('/agregar-recordatorio', verificarToken, async (req, res) => {
    try {
        const { fecha, tipoImpuesto, mensaje } = req.body;
        const usuarioId = req.user.uid;

        const recordatorio = new Recordatorio({ usuarioId, fecha, tipoImpuesto, mensaje });
        await recordatorio.save();
        res.status(201).send('Recordatorio agregado correctamente');
        
        enviarNotificacion(recordatorio);
    } catch (error) {
        console.error('Error al agregar el recordatorio:', error);
        res.status(500).send('Error al agregar el recordatorio');
    }
});

// Función para enviar una notificación push usando Firebase
const enviarNotificacion = async (recordatorio) => {
    try {
        const usuario = await Usuario.findOne({ usuarioId: recordatorio.usuarioId });
        if (!usuario || !usuario.token) {
            console.error('Token FCM no encontrado para el usuario:', recordatorio.usuarioId);
            return;
        }

        const mensaje = {
            notification: {
                title: `Recordatorio de ${recordatorio.tipoImpuesto}`,
                body: recordatorio.mensaje,
            },
            data: {
                clavePersonalizada: "valorEspecifico",
                tipoImpuesto: recordatorio.tipoImpuesto,
                fecha: recordatorio.fecha.toString(),
            },
            token: usuario.token,
        };

        await admin.messaging().send(mensaje);
        console.log('Notificación enviada exitosamente');
    } catch (error) {
        console.error('Error al enviar la notificación:', error);
    }
};

// Iniciar el servidor
app.listen(PORT, () => {
    console.log(`Servidor escuchando en http://localhost:${PORT}`);
});
