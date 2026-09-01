INSERT INTO platforms (code, display_name) VALUES ('BLINKIT', 'Blinkit') ON CONFLICT (code) DO NOTHING;
INSERT INTO platforms (code, display_name) VALUES ('ZEPTO', 'Zepto') ON CONFLICT (code) DO NOTHING;
INSERT INTO platforms (code, display_name) VALUES ('INSTAMART', 'Swiggy Instamart') ON CONFLICT (code) DO NOTHING;
INSERT INTO platforms (code, display_name) VALUES ('BIGBASKET', 'BigBasket') ON CONFLICT (code) DO NOTHING;
