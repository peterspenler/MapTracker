import tkinter as tk
from tkinter import ttk
from PIL import Image, ImageTk
import maptracker
import math
import sys

# Some code taken from 
# https://stackoverflow.com/questions/41656176/tkinter-canvas-zoom-move-pan
# and referenced
# https://stackoverflow.com/questions/14650605/text-fields-in-python-with-tkinter#14650945

def _create_circle(self, x, y, r, **kwargs):
    return self.create_oval(x-r, y-r, x+r, y+r, **kwargs)
tk.Canvas.create_circle = _create_circle

class DialogWindow:
    def finish(self):
        self.data = {'label': self.l.get(),
            'measuredx': self.mlx.get(),
            'measuredy': self.mly.get()}
        self.top.destroy()

    def __init__(self, parent, data=None):
        top = self.top = tk.Toplevel(parent)

        if data == None:
            data = {
                    'label': "",
                    'measuredx': '0.0',
                    'measuredy': '0.0'
                    }

        self.ll = tk.Label(top, text="Label")
        self.ll.pack()
        self.l = tk.Entry(top)
        self.l.pack()
        self.l.insert(0, data['label'])

        self.mlxl = tk.Label(top, text="Measured X Location")
        self.mlxl.pack()
        self.mlx = tk.Entry(top)
        self.mlx.pack()
        self.mlx.insert(0, data['measuredx'])

        self.mlyl = tk.Label(top, text="Measured Y Location")
        self.mlyl.pack()
        self.mly = tk.Entry(top)
        self.mly.pack()
        self.mly.insert(0, data['measuredy'])

        self.submit = tk.Button(top, text="Submit", command=self.finish)
        self.submit.pack()



class AutoScrollbar(ttk.Scrollbar):
    ''' A scrollbar that hides itself if it's not needed.
        Works only if you use the grid geometry manager '''
    def set(self, lo, hi):
        if float(lo) <= 0.0 and float(hi) >= 1.0:
            self.grid_remove()
        else:
            self.grid()
            ttk.Scrollbar.set(self, lo, hi)

    def pack(self, **kw):
        raise tk.TclError('Cannot use pack with this widget')

    def place(self, **kw):
        raise tk.TclError('Cannot use place with this widget')

class Zoom_Advanced(ttk.Frame):
    ''' Advanced zoom of the image '''
    def __init__(self, mainframe, path):
        ''' Initialize the main Frame '''
        ttk.Frame.__init__(self, master=mainframe)
        self.master.title('MapTracker Map Maker')
        # Vertical and horizontal scrollbars for canvas
        vbar = AutoScrollbar(self.master, orient='vertical')
        hbar = AutoScrollbar(self.master, orient='horizontal')
        vbar.grid(row=0, column=1, sticky='ns')
        hbar.grid(row=1, column=0, sticky='we')
        # Create canvas and put image on it
        self.canvas = tk.Canvas(self.master, highlightthickness=0,
                                xscrollcommand=hbar.set, yscrollcommand=vbar.set)
        self.canvas.grid(row=0, column=0, sticky='nswe')
        self.canvas.update()  # wait till canvas is created
        vbar.configure(command=self.scroll_y)  # bind scrollbars to the canvas
        hbar.configure(command=self.scroll_x)
        # Make the canvas expandable
        self.master.rowconfigure(0, weight=1)
        self.master.columnconfigure(0, weight=1)
        # Bind events to the Canvas
        self.canvas.bind('<Configure>', self.show_image)  # canvas is resized
        self.canvas.bind('<ButtonPress-1>', self.move_from)
        self.canvas.bind('<ButtonPress-2>', self.right_click)
        self.canvas.bind('<B1-Motion>',     self.move_to)
        self.master.bind('d', self.delete)
        self.image = Image.open(path)  # open image
        self.width, self.height = self.image.size
        self.imscale = 1.0  # scale for the canvaas image
        # Put image into container rectangle and use it to set proper coordinates to the image
        self.container = self.canvas.create_rectangle(0, 0, self.width, self.height, width=0)

        self.landmarks = []
        self.deleting = False

        self.show_image()

    def scroll_y(self, *args, **kwargs):
        ''' Scroll canvas vertically and redraw the image '''
        self.canvas.yview(*args, **kwargs)  # scroll vertically
        self.show_image()  # redraw the image

    def scroll_x(self, *args, **kwargs):
        ''' Scroll canvas horizontally and redraw the image '''
        self.canvas.xview(*args, **kwargs)  # scroll horizontally
        self.show_image()  # redraw the image

    def move_from(self, event):
        ''' Remember previous coordinates for scrolling with the mouse '''
        self.canvas.scan_mark(event.x, event.y)

    def find_landmark(self, clickx, clicky):
        for i, landmark in enumerate(self.landmarks):
            if abs(clickx - landmark.xdisplayloc) < 30 and \
               abs(clicky - landmark.ydisplayloc) < 30:
                return i
        return -1
    def right_click(self, event):
        clickx, clicky = event.x + self.canvas.canvasx(0), event.y + self.canvas.canvasy(0)
        lmi = self.find_landmark(clickx, clicky)
        if self.deleting:
            self.deleting = False
            if lmi == -1:
                return
            self.canvas.delete(self.landmarks[lmi].circle)
            del self.landmarks[lmi]
            self.show_image()
            return

        if lmi == -1:
            self.dialog_new(clickx, clicky)
        else:
            self.dialog_old(lmi)

        self.show_image()

    def delete(self, event):
        self.deleting = True
        print('Deleting')

    def dialog_new(self, clickx, clicky):
        dw = DialogWindow(self, data=None)
        self.master.wait_window(dw.top)
        data = dw.data
        temp = maptracker.MapConfigLandmark()
        temp.xdisplayloc = clickx
        temp.ydisplayloc = clicky
        temp.xloc = data['measuredx']
        temp.yloc = data['measuredy']
        temp.label = data['label']
        self.landmarks.append(temp)

    def dialog_old(self, index):
        lm = self.landmarks[index]
        dw = DialogWindow(self, data=lm.toMapInterface())
        self.master.wait_window(dw.top)
        data = dw.data
        temp = maptracker.MapConfigLandmark()
        temp.xdisplayloc = self.landmarks[index].xdisplayloc
        temp.ydisplayloc = self.landmarks[index].ydisplayloc
        temp.xloc = data['measuredx']
        temp.yloc = data['measuredy']
        temp.label = data['label']
        self.landmarks[index] = temp

    def move_to(self, event):
        ''' Drag (move) canvas to the new position '''
        self.canvas.scan_dragto(event.x, event.y, gain=1)
        self.show_image()  # redraw the image

    def show_image(self, event=None):
        ''' Show image on the Canvas '''
        bbox1 = self.canvas.bbox(self.container)  # get image area
        # Remove 1 pixel shift at the sides of the bbox1
        bbox1 = (bbox1[0] + 1, bbox1[1] + 1, bbox1[2] - 1, bbox1[3] - 1)
        bbox2 = (self.canvas.canvasx(0),  # get visible area of the canvas
                 self.canvas.canvasy(0),
                 self.canvas.canvasx(self.canvas.winfo_width()),
                 self.canvas.canvasy(self.canvas.winfo_height()))
        bbox = [min(bbox1[0], bbox2[0]), min(bbox1[1], bbox2[1]),  # get scroll region box
                max(bbox1[2], bbox2[2]), max(bbox1[3], bbox2[3])]
        if bbox[0] == bbox2[0] and bbox[2] == bbox2[2]:  # whole image in the visible area
            bbox[0] = bbox1[0]
            bbox[2] = bbox1[2]
        if bbox[1] == bbox2[1] and bbox[3] == bbox2[3]:  # whole image in the visible area
            bbox[1] = bbox1[1]
            bbox[3] = bbox1[3]
        self.canvas.configure(scrollregion=bbox)  # set scroll region
        x1 = max(bbox2[0] - bbox1[0], 0)  # get coordinates (x1,y1,x2,y2) of the image tile
        y1 = max(bbox2[1] - bbox1[1], 0)
        x2 = min(bbox2[2], bbox1[2]) - bbox1[0]
        y2 = min(bbox2[3], bbox1[3]) - bbox1[1]

        for landmark in self.landmarks:
            if hasattr(landmark, 'circle'):
                self.canvas.delete(landmark.circle)
            landmark.circle = self.canvas.create_circle(landmark.xdisplayloc, landmark.ydisplayloc, 20, fill="blue")

        if int(x2 - x1) > 0 and int(y2 - y1) > 0:  # show image if it in the visible area
            x = min(int(x2 / self.imscale), self.width)   # sometimes it is larger on 1 pixel...
            y = min(int(y2 / self.imscale), self.height)  # ...and sometimes not
            image = self.image.crop((int(x1 / self.imscale), int(y1 / self.imscale), x, y))
            imagetk = ImageTk.PhotoImage(image.resize((int(x2 - x1), int(y2 - y1))))
            imageid = self.canvas.create_image(max(bbox2[0], bbox1[0]), max(bbox2[1], bbox1[1]),
                                               anchor='nw', image=imagetk)
            self.canvas.lower(imageid)  # set image into background
            self.canvas.imagetk = imagetk  # keep an extra reference to prevent garbage-collection


def main(path):
    print("""Usage:
        <Left Click and Drag>   Move canvas
        <Right Click>   Add Landmark
        <d + Right Click> Remove Landmark
        Exit window to finish
        """)
    root = tk.Tk()
    app = Zoom_Advanced(root, path=path)
    root.mainloop()
    # Finish
    title = input('Give a title to this map: ')
    path = input('Give a URL (including http://) to this map for the image: ')

    cfg = maptracker.MapConfig()
    cfg.title = title
    cfg.imagepath = path
    cfg.landmarks = app.landmarks
    print("Output:\n{}\n".format(cfg.toJSON()))
    pathsave = input('Path to save to: ')
    with open(pathsave, 'w') as f:
        f.write(cfg.toJSON())
    print("Done!")



if __name__ == '__main__':
    if len(sys.argv) < 2:
        print('Usage: {} filename'.format(sys.argv[0]))
        sys.exit(1)
    main(sys.argv[1])
