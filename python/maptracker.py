
class MapConfigLandmark:
    def __init__(self, stored=None):
        self.label = ""
        self.xdisplayloc = 0
        self.ydisplayloc = 0
        self.xloc = 0.0
        self.yloc = 0.0
        if stored:
            self.label = stored['Label']
            self.xdisplayloc = stored['XDisplayLoc']
            self.ydisplayloc = stored['YDisplayLoc']
            self.xloc = stored['XLoc']
            self.yloc = stored['YLoc']

    def toMap(self):
        return {"Label": self.label,
                "XDisplayLoc": self.xdisplayloc,
                "YDisplayLoc": self.ydisplayloc,
                "XLoc": float(self.xloc),
                "YLoc": float(self.yloc)}

    def toMapInterface(self):
        return {"label": self.label,
                "measuredx": self.xloc,
                "measuredy": self.yloc}

class MapConfig:
    def __init__(self):
        self.title = ''
        self.imagepath = ''
        self.landmarks = [];

    def toMap(self):
        return {"Title": self.title,
                "ImagePath": self.imagepath,
                "Landmarks": list(map(lambda l: l.toMap(), self.landmarks))}

    def toJSON(self):
        import json
        return json.dumps(self.toMap())

    def __repr__(self):
        return self.toJSON(self)
